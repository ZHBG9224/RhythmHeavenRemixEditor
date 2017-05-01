package chrislo27.rhre

import chrislo27.rhre.credits.Credits.createConcatSections
import chrislo27.rhre.credits.CreditsScreen
import chrislo27.rhre.registry.Game
import chrislo27.rhre.registry.GameRegistry
import chrislo27.rhre.version.VersionChecker
import chrislo27.rhre.version.VersionState
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Array
import ionium.registry.ScreenRegistry
import ionium.transition.BlankTransition
import ionium.transition.Fade
import ionium.util.Utils
import ionium.util.i18n.Localization
import kotlin.concurrent.thread

class InfoScreen(m: Main) : NewUIScreen(m) {
	override var icon: String = "ui_info"
	override var title: String = "info.title"
	override var bottomInstructions: String = "info.instructions"

	companion object {
		private @Volatile var isChecking: Int = 0

		private @Volatile var inviteBacking: String? = null
		val invite: String
		get() {
			return inviteBacking ?: if (isChecking == -1) "[failed to get invite]" else "[getting...]"
		}

		init {
			if (isChecking == 0) {
				isChecking = 1
				thread(isDaemon = true) {
					try {
						inviteBacking = khttp.get(
								"https://raw.githubusercontent.com/chrislo27/RhythmHeavenRemixEditor2/dev/core/assets/data/dinvite.txt").text.trim()
						isChecking = 2
					} catch (e: Exception) {
						e.printStackTrace()
						isChecking = -1
					}
				}
			}
		}
	}

	private val patternCount: Int by lazy {
		GameRegistry.gameList.flatMap(Game::patterns).filter { !it.autoGenerated }.count()
	}

	private val soundCueCount: Int by lazy {
		GameRegistry.gameList.map { it.soundCues.size }.sum()
	}

	override fun render(delta: Float) {
		super.render(delta)

		main.batch.begin()

		val startX = main.camera.viewportWidth * 0.5f - BG_WIDTH * 0.5f
		val startY = main.camera.viewportHeight * 0.5f - BG_HEIGHT * 0.5f

		val url = "https://github.com/chrislo27/RhythmHeavenRemixEditor2"
		val urlLength = Utils.getWidth(main.font, url)
		val hoveringOverUrl = main.camera.viewportHeight - main.getInputY() <= startY + BG_HEIGHT * 0.75f &&
				main.camera.viewportHeight - main.getInputY() >= startY + BG_HEIGHT * 0.75f - main.font.capHeight &&
				main.getInputX() >= main.camera.viewportWidth * 0.5f - urlLength * 0.5f &&
				main.getInputX() <= main.camera.viewportWidth * 0.5f + urlLength * 0.5f
		main.font.setColor(0.5f, 0.65f, 1f, 1f)
		if (hoveringOverUrl) {
			main.font.setColor(0.6f, 0.75f, 1f, 1f)
		}
		main.font.draw(main.batch, url,
					   main.camera.viewportWidth * 0.5f,
					   startY + BG_HEIGHT * 0.75f, 0f, Align.center, false)
		main.font.draw(main.batch, "_________________________________________________________",
					   main.camera.viewportWidth * 0.5f,
					   startY + BG_HEIGHT * 0.75f, 0f, Align.center, false)

		main.font.setColor(1f, 1f, 1f, 1f)

		val stats: String = Localization.get("info.stats", "${GameRegistry.gameList.size}", "$patternCount",
											 "$soundCueCount")

		Main.drawCompressed(main.font, main.batch, stats,
							startX + BG_WIDTH * 0.55f,
							startY + BG_HEIGHT * 0.65f,
							BG_WIDTH * 0.4f,
							Align.center)

		main.font.data.setScale(0.75f)
		main.font.draw(main.batch,
					   Localization.get("info.discord", invite),
					   startX + BG_WIDTH * 0.05f,
					   startY + BG_HEIGHT * 0.675f,
					   BG_WIDTH * 0.4f,
					   Align.center, true)
		main.font.data.setScale(1f)

		Main.drawCompressed(main.font, main.batch, Localization.get("info.tocredits"),
							startX + PADDING,
							startY + BG_HEIGHT * 0.35f,
							BG_WIDTH - PADDING * 2,
							Align.center)

		main.font.data.setScale(0.75f)
		val license: String = Localization.get("info.credits.license")

		main.font.draw(main.batch, license,
					   startX + BG_WIDTH * 0.5f,
					   startY + BG_HEIGHT * 0.3f,
					   BG_WIDTH * 0.5f - PADDING,
					   Align.right, true)

		main.font.data.setScale(1f)

		val autosaveEnabled = main.preferences.getBoolean("autosave", true)
		main.font.draw(main.batch,
					   "[CYAN]A[] - " + Localization.get("info.autosave.${if (autosaveEnabled) "on" else "off"}"),
					   startX + PADDING,
					   startY + BG_HEIGHT * 0.2f,
					   BG_WIDTH * 0.5f - PADDING,
					   Align.left, true)

		if (Utils.isButtonJustPressed(Input.Buttons.LEFT) && hoveringOverUrl && main.screen === this) {
			Gdx.net.openURI(url)
		}

		main.batch.end()
	}

	override fun renderUpdate() {
		if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
			main.screen = ScreenRegistry.get("editor")
		} else if (Gdx.input.isKeyJustPressed(Input.Keys.V)) {
			if (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) || Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT)) {

			} else if (VersionChecker.versionState != VersionState.GETTING
					&& VersionChecker.versionState != VersionState.FAILED) {
				main.screen = ScreenRegistry.get("version")
			}
		} else if (Gdx.input.isKeyJustPressed(Input.Keys.A)) {
			main.preferences.putBoolean("autosave", !main.preferences.getBoolean("autosave", true))
			main.preferences.flush()
		} else if (Gdx.input.isKeyJustPressed(Input.Keys.C)) {
			main.transition(Fade(true, Color.rgba8888(0f, 0f, 0f, 1f), 1f),
							BlankTransition(0.5f, Color(0f, 0f, 0f, 1f)),
							CreditsScreen(main))
		}
	}

	override fun tickUpdate() {
	}

	override fun getDebugStrings(array: Array<String>?) {
	}

	override fun resize(width: Int, height: Int) {
	}

	override fun show() {
		createConcatSections()
	}

	override fun hide() {
	}

	override fun pause() {
	}

	override fun resume() {
	}

	override fun dispose() {
	}


}
