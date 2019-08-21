package com.template.states

import com.template.contracts.KycContract
import com.template.schema.KycSchemaV1
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState


// *********
// * State *
// *********
@BelongsToContract(KycContract::class)
 class KycState(val name: String = "not_shared", val address: String = "not_shared", val dob: String = "not_shared", val email: String ="not_shared",val owner: Party , val bank: Party,override val linearId: UniqueIdentifier = UniqueIdentifier() ) : LinearState , QueryableState {

       // get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val participants: List<AbstractParty> get() = listOf(owner,bank)
    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is KycSchemaV1 -> KycSchemaV1.PersistentKyc(
                    this.owner.name.toString(),
                    this.bank.name.toString(),
                    this.address,
                    this.linearId.id,
                    this.dob,
                    this. email,
                    this.name
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(KycSchemaV1)
}
