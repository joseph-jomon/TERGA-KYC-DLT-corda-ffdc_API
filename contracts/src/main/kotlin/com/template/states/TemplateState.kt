package com.template.states

import com.template.contracts.KycContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party



// *********
// * State *
// *********
@BelongsToContract(KycContract::class)
 class KycState(val name: String = "not_shared", val address: String = "not_shared", val dob: String = "not_shared", val email: String ="not_shared",val owner: Party , val bank: Party ) : ContractState {

    override val participants get() = listOf(owner,bank)
}
