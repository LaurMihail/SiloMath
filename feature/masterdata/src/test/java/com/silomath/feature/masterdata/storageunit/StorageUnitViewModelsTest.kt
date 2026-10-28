package com.silomath.feature.masterdata.storageunit

import androidx.lifecycle.SavedStateHandle
import com.silomath.core.domain.model.*
import com.silomath.core.domain.repository.*
import com.silomath.core.model.*
import com.silomath.feature.masterdata.location.LocationUnitsViewModel
import io.mockk.*
import java.time.Instant
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class StorageUnitViewModelsTest {
    private val org=OrganizationId("10000000-0000-0000-0000-000000000001")
    private val loc=LocationId("10000000-0000-0000-0000-000000000002")
    private val unit=StorageUnitId("10000000-0000-0000-0000-000000000003")
    private val actor="10000000-0000-0000-0000-000000000009"
    private val parent=Location(loc,org,"Location","L",null,null,null,null,MasterStatus.ACTIVE,Instant.EPOCH,actor,Instant.EPOCH,actor,1)
    private val existing=StorageUnit(unit,loc,"Unit","U","",MasterStatus.INACTIVE,Instant.EPOCH,actor,Instant.EPOCH,actor,7)
    private val session=MutableStateFlow(SessionState(userId=actor,token="test-only",activeOrganizationId=org.value,
        authorizedOrganizations=listOf(AuthorizedOrganizationContext("fixture",org.value,"Fixture","FIX","ADMIN"))))
    private val parents=MutableStateFlow(listOf(parent));private val rows=MutableStateFlow<List<StorageUnit>>(emptyList())
    private val children=MutableStateFlow<Result<List<StorageSpace>>>(Result.success(emptyList()))
    private val auth=mockk<AuthRepository>().also{every{it.sessionState} returns session}
    private val locations=mockk<LocationRepository>().also{every{it.observeLocations(org)} returns parents}
    private val units=mockk<StorageUnitRepository>().also{every{it.observeStorageUnits(loc)} returns rows}
    private val reader=mockk<StorageSpaceReader>().also{every{it.observe(org,loc,unit)} returns children}
    @Before fun setup(){Dispatchers.setMain(UnconfinedTestDispatcher());coEvery{units.hasHistoricalReferences(loc,unit)} returns Result.success(false)}
    @After fun cleanup(){Dispatchers.resetMain()}
    private fun editor(saved:SavedStateHandle=SavedStateHandle())=StorageUnitEditorViewModel(auth,locations,units,saved)
    @Test fun createUsesStableUuidAndSignalsSuccessOnlyAfterCommit()=runTest {
        val committed=CompletableDeferred<Result<Unit>>();val saved=slot<StorageUnit>()
        coEvery{units.saveStorageUnit(capture(saved))} coAnswers {committed.await()}
        val vm=editor();vm.bind(org,loc,null);vm.update(StorageUnitDraft(" New "," new "))
        val event=async{vm.savedEvents.first()};vm.save();runCurrent()
        assertTrue(vm.uiState.value.saving);assertFalse(event.isCompleted)
        assertEquals("New",saved.captured.name);assertEquals("NEW",saved.captured.code);assertEquals(loc,saved.captured.locationId)
        assertEquals(1L,saved.captured.entityVersion);assertNull(saved.captured.description)
        committed.complete(Result.success(Unit));runCurrent();event.await()
        assertFalse(vm.uiState.value.allowed)
    }
    @Test fun failureRetainsDraftSanitizesMessageAndReusesUuidAfterRecreation()=runTest {
        val captured=mutableListOf<StorageUnit>();coEvery{units.saveStorageUnit(capture(captured))} returns Result.failure(IllegalStateException("private diagnostic"))
        val saved=SavedStateHandle();val vm=editor(saved);vm.bind(org,loc,null);vm.update(StorageUnitDraft("New","NEW"))
        vm.save();runCurrent();assertEquals("New",vm.uiState.value.draft.name);assertFalse(vm.uiState.value.error!!.contains("private"))
        val restored=editor(saved);restored.bind(org,loc,null);restored.save();runCurrent()
        assertEquals(2,captured.size);assertEquals(captured[0].storageUnitId,captured[1].storageUnitId)
    }
    @Test fun editPreservesIdentityVersionCreationMetadataAndOptionalEmptyValue()=runTest {
        rows.value=listOf(existing);val captured=slot<StorageUnit>();coEvery{units.saveStorageUnit(capture(captured))} returns Result.success(Unit)
        val vm=editor();vm.bind(org,loc,unit);vm.update(vm.uiState.value.draft.copy(name="Renamed",status=MasterStatus.ACTIVE));vm.save();runCurrent()
        assertEquals(unit,captured.captured.storageUnitId);assertEquals(loc,captured.captured.locationId);assertEquals(7L,captured.captured.entityVersion)
        assertEquals(existing.createdAt,captured.captured.createdAt);assertEquals(existing.createdBy,captured.captured.createdBy)
        assertEquals("",captured.captured.description);assertEquals(MasterStatus.ACTIVE,captured.captured.lifecycleStatus)
    }
    @Test fun domainValidationPreventsInvalidNamesCodesAndDescriptionBeforeSave()=runTest {
        val vm=editor();vm.bind(org,loc,null)
        for(draft in listOf(StorageUnitDraft(),StorageUnitDraft("x".repeat(151),"U"),StorageUnitDraft("N","x".repeat(51)),StorageUnitDraft("N","U","x".repeat(501)))) {
            vm.update(draft);vm.save();runCurrent();assertNotNull(vm.uiState.value.error)
        }
        coVerify(exactly=0){units.saveStorageUnit(any())}
    }
    @Test fun standardForgedContextRevokedAdminAndMissingParentCannotMutate()=runTest {
        session.value=session.value.copy(authorizedOrganizations=session.value.authorizedOrganizations.map{it.copy(role="STANDARD")})
        val denied=editor();denied.bind(org,loc,null);denied.update(StorageUnitDraft("N","U"));denied.save();runCurrent();assertFalse(denied.uiState.value.allowed)
        session.value=session.value.copy(authorizedOrganizations=session.value.authorizedOrganizations.map{it.copy(role="ADMIN")})
        val vm=editor();vm.bind(org,loc,null);assertTrue(vm.uiState.value.allowed)
        session.value=session.value.copy(token=null);vm.save();runCurrent();assertFalse(vm.uiState.value.allowed)
        coVerify(exactly=0){units.saveStorageUnit(any())}
    }
    @Test fun historicalCodeIsReadOnlyAndChangedOriginalBlocksStaleSave()=runTest {
        rows.value=listOf(existing);coEvery{units.hasHistoricalReferences(loc,unit)} returns Result.success(true)
        val vm=editor();vm.bind(org,loc,unit);assertTrue(vm.uiState.value.codeLocked)
        vm.update(vm.uiState.value.draft.copy(code="OTHER"));assertEquals("U",vm.uiState.value.draft.code)
        rows.value=listOf(existing.copy(entityVersion=8));runCurrent();assertFalse(vm.uiState.value.allowed)
        vm.save();runCurrent();coVerify(exactly=0){units.saveStorageUnit(any())}
    }
    @Test fun detailsDistinguishesTrueEmptyFromReadFailureAndRevokesProtectedContent()=runTest {
        rows.value=listOf(existing);val vm=StorageUnitDetailsViewModel(auth,locations,units,reader)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)){vm.uiState.collect{}}
        vm.bind(org,loc,unit);runCurrent();assertEquals(existing,vm.uiState.value.unit);assertNull(vm.uiState.value.spacesError);assertTrue(vm.uiState.value.spaces.isEmpty())
        children.value=Result.failure(IllegalStateException("private"));runCurrent();assertNotNull(vm.uiState.value.spacesError)
        session.value=session.value.copy(authorizedOrganizations=emptyList());runCurrent();assertNull(vm.uiState.value.unit)
    }
    @Test fun locationUnitsRejectsWrongParentAndRoleWithoutRemoteCalls()=runTest {
        rows.value=listOf(existing,existing.copy(storageUnitId=StorageUnitId("10000000-0000-0000-0000-000000000004"),locationId=LocationId("10000000-0000-0000-0000-000000000005")))
        val vm=LocationUnitsViewModel(auth,locations,units);backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)){vm.uiState.collect{}}
        vm.bind(org,loc);runCurrent();assertEquals(listOf(existing),vm.uiState.value.units)
        parents.value=emptyList();runCurrent();assertNotNull(vm.uiState.value.error)
        coVerify(exactly=0){units.refreshStorageUnits(loc)}
    }
}
