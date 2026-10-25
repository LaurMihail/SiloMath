package com.silomath.core.domain.model

import org.junit.Assert.assertThrows
import org.junit.Test

class SyncDomainRulesTest {

    @Test
    fun `pending dispatches only to in flight`() {
        SyncDomainRules.validateOperationTransition(
            SyncOperationState.PENDING,
            SyncOperationState.IN_FLIGHT
        )
        assertThrows(IllegalArgumentException::class.java) {
            SyncDomainRules.validateOperationTransition(
                SyncOperationState.PENDING,
                SyncOperationState.ACKNOWLEDGED
            )
        }
    }

    @Test
    fun `in flight permits only frozen result states plus crash recovery`() {
        listOf(
            SyncOperationState.ACKNOWLEDGED,
            SyncOperationState.RETRYABLE_ERROR,
            SyncOperationState.CONFLICT,
            SyncOperationState.PERMANENT_ERROR,
            SyncOperationState.PENDING
        ).forEach { target ->
            SyncDomainRules.validateOperationTransition(
                SyncOperationState.IN_FLIGHT,
                target
            )
        }
    }

    @Test
    fun `retryable error returns to pending with same durable operation`() {
        SyncDomainRules.validateOperationTransition(
            SyncOperationState.RETRYABLE_ERROR,
            SyncOperationState.PENDING
        )
        assertThrows(IllegalArgumentException::class.java) {
            SyncDomainRules.validateOperationTransition(
                SyncOperationState.RETRYABLE_ERROR,
                SyncOperationState.IN_FLIGHT
            )
        }
    }

    @Test
    fun `acknowledged is the only successful terminal state`() {
        SyncDomainRules.requireSuccessfulTerminal(SyncOperationState.ACKNOWLEDGED)
        listOf(
            SyncOperationState.CONFLICT,
            SyncOperationState.PERMANENT_ERROR,
            SyncOperationState.RETRYABLE_ERROR
        ).forEach { state ->
            assertThrows(IllegalArgumentException::class.java) {
                SyncDomainRules.requireSuccessfulTerminal(state)
            }
        }
    }

    @Test
    fun `conflict lifecycle is exactly open to resolved`() {
        SyncDomainRules.validateConflictTransition(
            SyncConflictStatus.OPEN,
            SyncConflictStatus.RESOLVED
        )
        assertThrows(IllegalArgumentException::class.java) {
            SyncDomainRules.validateConflictTransition(
                SyncConflictStatus.RESOLVED,
                SyncConflictStatus.OPEN
            )
        }
    }
}
