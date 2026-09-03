package com.snapgrade.app

import com.snapgrade.app.ai.GradingEngine
import com.snapgrade.app.data.model.DemoRubrics
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class GradingEngineTest {

    private lateinit var engine: GradingEngine

    @Before
    fun setUp() {
        engine = GradingEngine()
    }

    @Test
    fun testPhotosynthesisHighScorerEvaluation() {
        val rubric = DemoRubrics.PHOTOSYNTHESIS
        val highScorerText = rubric.sampleAnswers[0].text

        val result = engine.evaluateWithEdgeSemanticEngine(highScorerText, rubric)

        assertEquals(rubric.id, result.rubricId)
        assertEquals(3, result.criterionScores.size)
        assertTrue("Total awarded points should be >= 7, but was ${result.totalAwardedPoints}", result.totalAwardedPoints >= 7)
        assertTrue("Total points should not exceed max", result.totalAwardedPoints <= result.totalMaxPoints)

        // Check specific criterion
        val lightCrit = result.criterionScores.first { it.criterionId == "crit_light" }
        assertTrue("Light reaction should receive points", lightCrit.awardedPoints >= 2)
        assertTrue("Feedback should not be blank", lightCrit.feedback.isNotBlank())
    }

    @Test
    fun testPhotosynthesisPartialCreditEvaluation() {
        val rubric = DemoRubrics.PHOTOSYNTHESIS
        val partialText = rubric.sampleAnswers[1].text

        val result = engine.evaluateWithEdgeSemanticEngine(partialText, rubric)

        assertTrue(
            "Partial scorer should receive lower points than high scorer",
            result.totalAwardedPoints < rubric.totalMaxPoints
        )
        assertTrue("Should still receive some credit", result.totalAwardedPoints > 0)
    }

    @Test
    fun testNewtonThirdLawEvaluation() {
        val rubric = DemoRubrics.NEWTON_THIRD_LAW
        val highScorerText = rubric.sampleAnswers[0].text

        val result = engine.evaluateWithEdgeSemanticEngine(highScorerText, rubric)

        assertEquals(3, result.criterionScores.size)
        assertTrue("Newton answer should score >= 6", result.totalAwardedPoints >= 6)

        val exampleCrit = result.criterionScores.first { it.criterionId == "crit_example" }
        assertTrue("Example should be awarded points for rocket example", exampleCrit.awardedPoints >= 2)
    }

    @Test
    fun testPromptGenerationContainsSchemaAndCriteria() {
        val rubric = DemoRubrics.PHOTOSYNTHESIS
        val prompt = engine.buildGradingPrompt("Chlorophyll absorbs light", rubric)

        assertTrue("Prompt should mention rubric title", prompt.contains("Photosynthesis"))
        assertTrue("Prompt should include criterion ID", prompt.contains("crit_light"))
        assertTrue("Prompt should enforce JSON format", prompt.contains("\"criteria_scores\""))
    }

    @Test
    fun testParseLlmJsonOutput() {
        val rubric = DemoRubrics.PHOTOSYNTHESIS
        val simulatedJson = """
        {
          "rubric_id": "rubric_photosynthesis",
          "criteria_scores": [
            {
              "criterion_id": "crit_light",
              "criterion_name": "Light-Dependent Reactions",
              "awarded_points": 4,
              "max_points": 4,
              "feedback": "Correctly detailed thylakoids and water splitting."
            },
            {
              "criterion_id": "crit_calvin",
              "criterion_name": "Calvin Cycle / Dark Reaction",
              "awarded_points": 3,
              "max_points": 4,
              "feedback": "Covered carbon fixation well."
            },
            {
              "criterion_id": "crit_clarity",
              "criterion_name": "Scientific Clarity & Terminology",
              "awarded_points": 2,
              "max_points": 2,
              "feedback": "Well-structured scientific explanation."
            }
          ],
          "general_feedback": "Overall very strong demonstration of photosynthetic concepts."
        }
        """.trimIndent()

        val parsed = engine.parseLlmJsonOutput(simulatedJson, rubric)
        assertNotNull(parsed)
        assertEquals(9, parsed!!.totalAwardedPoints)
        assertEquals(10, parsed.totalMaxPoints)
        assertEquals("crit_light", parsed.criterionScores[0].criterionId)
        assertEquals(4, parsed.criterionScores[0].awardedPoints)
    }
}
