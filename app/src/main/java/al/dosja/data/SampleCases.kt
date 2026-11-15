package al.dosja.data

data class CaseDoc(
    val id: String,
    val title: String,
    val kind: String,
    val text: String
)

/**
 * Three documents that ship with the app so a judge (or a lawyer) can try the
 * routing logic without importing a real case file first.
 */
object SampleCases {

    val all: List<CaseDoc> = listOf(
        CaseDoc(
            id = "short-lease",
            title = "Lease agreement — Art. 4 (rent review)",
            kind = "Contract · 2 pages",
            text = """
                Article 4 — Rent review
                4.1 The monthly rent is EUR 640, payable by the fifth day of each month.
                4.2 The landlord may review the rent once every twelve months, provided
                written notice is given at least sixty days before the review takes effect.
                4.3 Any review may not exceed the annual change in the official consumer
                price index published by the statistical office.
                Article 5 — Termination
                5.1 Either party may terminate with ninety days written notice.
                5.2 Early termination by the tenant requires payment of one month's rent
                in lieu of notice.
            """.trimIndent()
        ),
        CaseDoc(
            id = "medium-employment",
            title = "Employment contract — probation and non-compete",
            kind = "Contract · 6 pages",
            text = """
                Article 2 — Probation
                2.1 The first three months of employment are a probation period.
                2.2 During probation either party may terminate with seven days notice.
                Article 6 — Non-compete
                6.1 For twelve months after termination the employee shall not work for a
                competitor operating within the same market segment in the territory.
                6.2 In consideration of this restriction the employer pays 30% of the
                average monthly salary for the duration of the restriction.
                6.3 The restriction does not apply if the employer terminates without
                cause, or if the employer fails to pay the compensation for two
                consecutive months.
                Article 9 — Working time
                9.1 Normal working time is 40 hours per week.
                9.2 Overtime is compensated at 130% of the hourly rate.
            """.trimIndent()
        ),
        CaseDoc(
            id = "long-filing",
            title = "Statement of claim — construction dispute (scanned)",
            kind = "Filing · 34 pages · OCR text",
            text = (1..120).joinToString("\n") { i ->
                "Clause $i of the statement of claim records that the parties disagreed about the " +
                    "delivery schedule for phase $i of the works, and that the claimant notified the " +
                    "respondent in writing on the date recorded in annex $i of the file."
            }
        )
    )
}
