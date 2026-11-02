package com.silomath.feature.masterdata.location

import androidx.lifecycle.SavedStateHandle
import com.silomath.core.domain.model.*
import com.silomath.core.domain.repository.*
import com.silomath.core.model.*
import io.mockk.*
import java.time.Instant
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class LocationEditorViewModelTest {
    private val org=OrganizationId("00000000-0000-0000-0000-000000000001")
    private val id=LocationId("10000000-0000-0000-0000-000000000001")
    private val session=MutableStateFlow(SessionState(userId="00000000-0000-0000-0000-000000000003",token="test-only",
        activeOrganizationId=org.value,authorizedOrganizations=listOf(AuthorizedOrganizationContext("fixture",org.value,"Fixture","FIX","ADMIN"))))
    private val rows=MutableStateFlow<List<Location>>(emptyList())
    private val auth=mockk<AuthRepository>().also{every{it.sessionState} returns session}
    private val repo=mockk<LocationRepository>().also{every{it.observeLocations(org)} returns rows}
    private fun existing(country:String?="RO")=Location(id,org,"North","N",null,null,country,null,MasterStatus.ACTIVE,
        Instant.EPOCH,"fixture-creator",Instant.EPOCH,"fixture-updater",7)
    @Before fun setup(){Dispatchers.setMain(UnconfinedTestDispatcher());coEvery{repo.hasHistoricalReferences(org,id)} returns Result.success(false)}
    @After fun teardown(){Dispatchers.resetMain()}
    @Test fun createUsesCanonicalSelectionAndSignalsSuccessOnlyAfterDurableSave()=runTest {
        val committed=CompletableDeferred<Result<Unit>>()
        val saved=slot<Location>();coEvery{repo.saveLocation(capture(saved))} coAnswers {committed.await()}
        val vm=LocationEditorViewModel(auth,repo,SavedStateHandle());vm.bind(org,null)
        vm.update(LocationDraft(name="  New Location  ",code=" new ",country="DE",latitude="-90",longitude="180"))
        val event=async{vm.savedEvents.first()};vm.save();runCurrent()
        assertTrue(vm.uiState.value.saving);assertFalse(event.isCompleted)
        assertEquals("New Location",saved.captured.name);assertEquals("NEW",saved.captured.code)
        assertEquals("DE",saved.captured.country);assertEquals(org,saved.captured.organizationId)
        assertEquals(session.value.userId,saved.captured.createdBy)
        committed.complete(Result.success(Unit));runCurrent();event.await()
        assertFalse(vm.uiState.value.saving)
    }
    @Test fun saveFailureRetainsDraftAndNeverEmitsSuccess()=runTest {
        coEvery{repo.saveLocation(any())} returns Result.failure(IllegalStateException("private diagnostic"))
        val vm=LocationEditorViewModel(auth,repo,SavedStateHandle());vm.bind(org,null)
        vm.update(LocationDraft(name="New",code="NEW"));val event=async{vm.savedEvents.first()}
        vm.save();runCurrent();assertFalse(event.isCompleted);event.cancel()
        assertEquals("New",vm.uiState.value.draft.name);assertFalse(vm.uiState.value.error!!.contains("private"))
    }
    @Test fun standardAndRevokedAdminCannotMutate()=runTest {
        val vm=LocationEditorViewModel(auth,repo,SavedStateHandle());vm.bind(org,null)
        vm.update(LocationDraft(name="New",code="NEW"))
        session.value=session.value.copy(authorizedOrganizations=session.value.authorizedOrganizations.map{it.copy(role="STANDARD")})
        vm.save();runCurrent();assertFalse(vm.uiState.value.allowed)
        coVerify(exactly=0){repo.saveLocation(any())}
    }
    @Test fun historyLocksCodeAndEditPreservesIdentityVersionAndCreationMetadata()=runTest {
        rows.value=listOf(existing());coEvery{repo.hasHistoricalReferences(org,id)} returns Result.success(true)
        val captured=slot<Location>();coEvery{repo.saveLocation(capture(captured))} returns Result.success(Unit)
        val vm=LocationEditorViewModel(auth,repo,SavedStateHandle());vm.bind(org,id)
        assertTrue(vm.uiState.value.codeLocked)
        vm.update(vm.uiState.value.draft.copy(code="CHANGED"));vm.save();runCurrent()
        assertNotNull(vm.uiState.value.errors["code"]);coVerify(exactly=0){repo.saveLocation(any())}
        vm.update(vm.uiState.value.draft.copy(code="N",name="Renamed",status=MasterStatus.INACTIVE));vm.save();runCurrent()
        assertEquals(id,captured.captured.locationId);assertEquals(7L,captured.captured.entityVersion)
        assertEquals(Instant.EPOCH,captured.captured.createdAt);assertEquals("fixture-creator",captured.captured.createdBy)
        assertEquals(MasterStatus.INACTIVE,captured.captured.lifecycleStatus)
    }
    @Test fun historicalCountryRemainsVerbatimAndInvalidNewValuesFailClosed()=runTest {
        rows.value=listOf(existing("Romania"));val captured=slot<Location>()
        coEvery{repo.saveLocation(capture(captured))} returns Result.success(Unit)
        val vm=LocationEditorViewModel(auth,repo,SavedStateHandle());vm.bind(org,id)
        vm.update(vm.uiState.value.draft.copy(country="ro"));vm.save();runCurrent()
        assertNotNull(vm.uiState.value.errors["country"])
        vm.update(vm.uiState.value.draft.copy(country="Romania",name="Renamed"));vm.save();runCurrent()
        assertEquals("Romania",captured.captured.country)
        assertFalse(CountryCatalog.isAllowedNewValue("Romania"));assertTrue(CountryCatalog.isAllowedNewValue(null))
    }
    @Test fun staleSourceAndUnverifiableHistoryDoNotUnlockEdit()=runTest {
        rows.value=listOf(existing());val vm=LocationEditorViewModel(auth,repo,SavedStateHandle());vm.bind(org,id)
        vm.update(vm.uiState.value.draft.copy(name="Draft"));rows.value=listOf(existing().copy(entityVersion=8))
        assertFalse(vm.uiState.value.allowed);vm.save();runCurrent();coVerify(exactly=0){repo.saveLocation(any())}
        coEvery{repo.hasHistoricalReferences(org,id)} returns Result.failure(IllegalStateException("missing context"))
        val blocked=LocationEditorViewModel(auth,repo,SavedStateHandle());blocked.bind(org,id)
        assertFalse(blocked.uiState.value.allowed)
    }
    @Test fun validationCoversLimitsUniquenessOptionalNullsAndCoordinatePair() {
        val bad=LocationDraft(name=" ",code=" n ",address="x".repeat(251),city="x".repeat(101),notes="x".repeat(501),
            country="ZZ",latitude="91",longitude="")
        assertEquals(setOf("name","code","address","city","notes","country","latitude","longitude"),bad.errors(null,listOf(existing()),false).keys)
        assertTrue(LocationDraft(name="Good",code="NEW").errors(null,emptyList(),false).isEmpty())
        assertTrue(LocationDraft(name="Good",code="NEW",latitude="-90",longitude="-180").errors(null,emptyList(),false).isEmpty())
    }
    @Test fun restoredDraftPreservesHistoricalEmptyCountryWithoutImplicitNormalization()=runTest {
        rows.value=listOf(existing(""));val saved=SavedStateHandle()
        val vm=LocationEditorViewModel(auth,repo,saved);vm.bind(org,id);vm.update(vm.uiState.value.draft.copy(name="Draft"))
        val restored=LocationEditorViewModel(auth,repo,saved);restored.bind(org,id)
        assertEquals("",restored.uiState.value.draft.country);assertEquals("Draft",restored.uiState.value.draft.name)
    }
}
