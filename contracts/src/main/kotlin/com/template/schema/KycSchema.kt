package com.template.schema

import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

/**
 * The family of schemas for IOUState.
 */
object KycSchema

/**
 * An KycState schema.
 */
object KycSchemaV1 : MappedSchema(
        schemaFamily = KycSchema.javaClass,
        version = 1,
        mappedTypes = listOf(KycSchemaV1.PersistentKyc::class.java)) {
    @Entity
    @Table(name = "kyc_states")
    class PersistentKyc(
            @Column(name = "owner")
            var ownerName: String,

            @Column(name = "bank")
            var bankName: String,

            @Column(name = "name")
            var name: String,

            @Column(name = "linear_id")
            var linearId: UUID,

            @Column(name = "address")
            var address: String,

            @Column(name = "dob")
            var dob: String,
            @Column(name = "email")
             var email: String



    ) : PersistentState() {
        // Default constructor required by hibernate.
        constructor(): this("", "", "", UUID.randomUUID(), "", "", "")
    }
}