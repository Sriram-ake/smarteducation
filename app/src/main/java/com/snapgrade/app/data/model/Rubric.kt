package com.snapgrade.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * Criterion definition for a grading rubric.
 */
data class Criterion(
    val id: String,
    val name: String,
    val description: String,
    val maxPoints: Int,
    val keyConcepts: List<String> = emptyList()
)

/**
 * Rubric definition holding multiple criteria.
 */
data class Rubric(
    val id: String,
    val title: String,
    val subject: String,
    val assignmentPrompt: String,
    val criteria: List<Criterion>,
    val sampleAnswers: List<SampleAnswer> = emptyList()
) {
    val totalMaxPoints: Int
        get() = criteria.sumOf { it.maxPoints }
}

/**
 * Pre-recorded sample student answer for quick demos and emulator testing.
 */
data class SampleAnswer(
    val studentName: String,
    val text: String,
    val description: String
)

/**
 * Score and constructive feedback for an individual criterion.
 */
data class CriterionScore(
    @SerializedName("criterion_id")
    val criterionId: String,
    @SerializedName("criterion_name")
    val criterionName: String,
    @SerializedName("awarded_points")
    var awardedPoints: Int,
    @SerializedName("max_points")
    val maxPoints: Int,
    @SerializedName("feedback")
    var feedback: String
)

/**
 * Complete evaluation result from the on-device AI pipeline.
 */
data class GradingResult(
    val rubricId: String,
    val rubricTitle: String,
    var criterionScores: List<CriterionScore>,
    var generalFeedback: String,
    val ocrLatencyMs: Long = 0L,
    val inferenceLatencyMs: Long = 0L,
    val engineName: String = "Snapdragon NPU (On-Device)"
) {
    val totalAwardedPoints: Int
        get() = criterionScores.sumOf { it.awardedPoints }

    val totalMaxPoints: Int
        get() = criterionScores.sumOf { it.maxPoints }

    val percentage: Float
        get() = if (totalMaxPoints > 0) (totalAwardedPoints.toFloat() / totalMaxPoints) * 100f else 0f

    val gradeBadge: String
        get() = when {
            percentage >= 90f -> "A+ (Outstanding)"
            percentage >= 80f -> "A (Proficient)"
            percentage >= 70f -> "B (Competent)"
            percentage >= 60f -> "C (Developing)"
            else -> "Needs Improvement"
        }
}

/**
 * Built-in demo rubrics with realistic assignment prompts and sample handwritten texts.
 */
object DemoRubrics {

    val PHOTOSYNTHESIS = Rubric(
        id = "rubric_photosynthesis",
        title = "Photosynthesis Mechanism",
        subject = "Biology (Grade 10)",
        assignmentPrompt = "Explain how plants convert solar energy into glucose. Include both the light-dependent reactions and the Calvin cycle, noting key inputs and outputs.",
        criteria = listOf(
            Criterion(
                id = "crit_light",
                name = "Light-Dependent Reactions",
                description = "Explains sunlight absorption by chlorophyll, photolysis of water, and generation of ATP & NADPH with oxygen release.",
                maxPoints = 4,
                keyConcepts = listOf("chlorophyll", "light", "water", "oxygen", "atp", "nadph", "thylakoid")
            ),
            Criterion(
                id = "crit_calvin",
                name = "Calvin Cycle / Dark Reaction",
                description = "Explains carbon fixation of CO2 in the stroma and synthesis of glucose / carbohydrates.",
                maxPoints = 4,
                keyConcepts = listOf("calvin cycle", "carbon dioxide", "co2", "glucose", "stroma", "sugar", "carbohydrate")
            ),
            Criterion(
                id = "crit_clarity",
                name = "Scientific Clarity & Terminology",
                description = "Clear structure, logical flow, and correct scientific vocabulary without misleading contradictions.",
                maxPoints = 2,
                keyConcepts = listOf("chloroplast", "energy", "conversion", "process", "equation")
            )
        ),
        sampleAnswers = listOf(
            SampleAnswer(
                studentName = "Arjun Patel (High Scorer)",
                text = "Photosynthesis takes place in chloroplasts. In the thylakoid membranes, chlorophyll captures sunlight during light-dependent reactions. Water is split to produce oxygen, ATP, and NADPH. Next, in the stroma, the Calvin cycle fixes carbon dioxide using ATP and NADPH energy to synthesize glucose for the plant.",
                description = "Covers all key reactions, molecules, and organelles accurately."
            ),
            SampleAnswer(
                studentName = "Rhea Sharma (Partial Credit)",
                text = "Plants make food using sunlight. Chlorophyll in green leaves absorbs sun and takes water from roots to give off oxygen. Then it uses carbon dioxide to create sugars and energy.",
                description = "Good general understanding, but omits mention of Calvin cycle, ATP/NADPH, or stroma."
            )
        )
    )

    val NEWTON_THIRD_LAW = Rubric(
        id = "rubric_newton",
        title = "Newton's Third Law of Motion",
        subject = "Physics (Grade 9)",
        assignmentPrompt = "State Newton's Third Law of Motion. Explain why action-reaction pairs do not cancel each other out, and illustrate with a real-world example.",
        criteria = listOf(
            Criterion(
                id = "crit_statement",
                name = "Law Statement & Symmetry",
                description = "Accurately states that for every action force, there is an equal and opposite reaction force.",
                maxPoints = 4,
                keyConcepts = listOf("equal", "opposite", "action", "reaction", "force", "direction")
            ),
            Criterion(
                id = "crit_distinct_bodies",
                name = "Action-Reaction on Separate Bodies",
                description = "Clarifies that action and reaction forces act on two DIFFERENT objects, which is why they do not cancel out into equilibrium.",
                maxPoints = 3,
                keyConcepts = listOf("different bodies", "separate objects", "two objects", "do not cancel", "distinct")
            ),
            Criterion(
                id = "crit_example",
                name = "Real-World Application",
                description = "Provides a sound, realistic example (e.g., rocket propulsion pushing exhaust gases, swimmer pushing water backward).",
                maxPoints = 3,
                keyConcepts = listOf("rocket", "swimming", "walking", "exhaust", "ground", "propulsion", "push")
            )
        ),
        sampleAnswers = listOf(
            SampleAnswer(
                studentName = "David Chen (High Scorer)",
                text = "Newton's Third Law states that every action has an equal and opposite reaction. These forces do not cancel out because they act on two different bodies simultaneously. For example, a rocket pushes exhaust gases downward (action), and the escaping gas pushes the rocket upward (reaction).",
                description = "Addresses all three criteria clearly with a correct rocket propulsion example."
            ),
            SampleAnswer(
                studentName = "Sara Khan (Common Misconception)",
                text = "Newton's 3rd law means forces are always equal and opposite. When you push a wall it pushes back. Since they are equal, net force is zero and nothing moves.",
                description = "States the law but exhibits misconception regarding net force cancellation."
            )
        )
    )

    val CAUSE_AND_EFFECT = Rubric(
        id = "rubric_history",
        title = "The Industrial Revolution",
        subject = "History / Social Science",
        assignmentPrompt = "Discuss the primary causes of the Industrial Revolution and analyze two major socio-economic consequences.",
        criteria = listOf(
            Criterion(
                id = "crit_causes",
                name = "Primary Causes & Innovations",
                description = "Identifies key drivers such as the steam engine, mechanization of textiles, coal/iron reserves, and agricultural surplus.",
                maxPoints = 4,
                keyConcepts = listOf("steam engine", "coal", "textile", "mechanization", "iron", "agriculture", "watt")
            ),
            Criterion(
                id = "crit_consequences",
                name = "Socio-Economic Impacts",
                description = "Analyzes consequences like urbanization, factory labor conditions, rise of the middle class, or shifts in family life.",
                maxPoints = 4,
                keyConcepts = listOf("urbanization", "cities", "factory", "labor", "working conditions", "economy", "middle class")
            ),
            Criterion(
                id = "crit_expression",
                name = "Coherence & Argumentation",
                description = "Well-structured prose, logical argument linking causes directly to effects.",
                maxPoints = 2,
                keyConcepts = listOf("led to", "consequently", "transformed", "result", "society")
            )
        ),
        sampleAnswers = listOf(
            SampleAnswer(
                studentName = "Elena Rostova (Strong Analysis)",
                text = "The Industrial Revolution began in Britain due to rich coal deposits and inventions like the James Watt steam engine and mechanized loom. Consequently, societies experienced rapid urbanization as people moved to cities for factory jobs. However, this also led to harsh working conditions and child labor before labor reforms emerged.",
                description = "Thorough analysis of technological catalysts and demographic consequences."
            )
        )
    )

    val ALL = listOf(PHOTOSYNTHESIS, NEWTON_THIRD_LAW, CAUSE_AND_EFFECT)
}
