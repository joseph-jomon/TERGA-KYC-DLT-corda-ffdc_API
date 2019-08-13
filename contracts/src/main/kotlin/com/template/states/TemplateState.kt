package com.template.states

import com.template.contracts.TemplateContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party



// *********
// * State *
// *********
@BelongsToContract(TemplateContract::class)
 class KycState(val name: String, val address: String , val dob: String, val email: String,val owner: Party, val bank: Party) : ContractState {

    override val participants get() = listOf(owner,bank)
}
