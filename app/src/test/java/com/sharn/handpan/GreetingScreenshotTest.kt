package com.sharn.handpan

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.sharn.handpan.ui.components.HandpanDiscView
import com.sharn.handpan.model.HandpanPattern
import com.sharn.handpan.model.LearningRecommendation
import com.sharn.handpan.model.LearningSkill
import com.sharn.handpan.model.PracticeRecommendation
import com.sharn.handpan.ui.screens.RecommendationCard
import com.sharn.handpan.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertEquals
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun handpan_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme {
        Box(modifier = Modifier.size(360.dp)) {
          HandpanDiscView(activeNoteNumber = 1, isInteractive = false)
        }
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/handpan_disc.png")
  }

  @Test
  fun recommendation_card_displays_pattern_and_returns_same_pattern() {
    val pattern = com.sharn.handpan.data.builtin.BuiltinExercises.ALL_BUILTIN_PATTERNS.first()
    var startedPattern: HandpanPattern? = null

    composeTestRule.setContent {
      MyApplicationTheme {
        RecommendationCard(
          pattern = pattern,
          recommendation = LearningRecommendation(
            skill = LearningSkill.NOTE_ACCURACY,
            reason = PracticeRecommendation.FOCUS_WEAKNESS,
            patternId = pattern.id
          ),
          onStart = { startedPattern = pattern }
        )
      }
    }

    composeTestRule.onNodeWithTag("learning_recommendation_card").assertIsDisplayed()
    composeTestRule.onNodeWithText(pattern.title).assertIsDisplayed()
    composeTestRule.onNodeWithTag("learning_recommendation_start_button").performClick()
    assertEquals(pattern.id, startedPattern?.id)
  }
}
