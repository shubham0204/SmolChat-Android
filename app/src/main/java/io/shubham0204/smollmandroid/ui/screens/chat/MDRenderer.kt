package io.shubham0204.smollmandroid.ui.screens.chat

import android.content.Context
import android.graphics.Color
import android.text.Spanned
import android.text.util.Linkify
import android.util.TypedValue
import androidx.core.content.res.ResourcesCompat
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.core.CorePlugin
import io.noties.markwon.core.MarkwonTheme
import io.noties.markwon.ext.latex.JLatexMathPlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.inlineparser.MarkwonInlineParserPlugin
import io.noties.markwon.linkify.LinkifyPlugin
import io.noties.markwon.syntax.Prism4jThemeDarkula
import io.noties.markwon.syntax.SyntaxHighlightPlugin
import io.noties.prism4j.Prism4j
import io.shubham0204.smollmandroid.R
import io.shubham0204.smollmandroid.prism4j.PrismGrammarLocator
import org.koin.core.annotation.Single

@Single
class MDRenderer(private val context: Context) {
    private val prism4j = Prism4j(PrismGrammarLocator())
    private val markwon =
        Markwon.builder(context)
            .usePlugin(CorePlugin.create())
            .usePlugin(SyntaxHighlightPlugin.create(prism4j, Prism4jThemeDarkula.create()))
            .usePlugin(MarkwonInlineParserPlugin.create())
            .usePlugin(
                JLatexMathPlugin.create(
                    12f,
                    JLatexMathPlugin.BuilderConfigure {
                        it.inlinesEnabled(true)
                        it.blocksEnabled(true)
                    },
                )
            )
            .usePlugin(LinkifyPlugin.create(Linkify.WEB_URLS))
            .usePlugin(HtmlPlugin.create())
            .usePlugin(
                object : AbstractMarkwonPlugin() {
                    override fun configureTheme(builder: MarkwonTheme.Builder) {
                        val jetbrainsMonoFont =
                            ResourcesCompat.getFont(context, R.font.jetbrains_mono)!!
                        builder
                            .codeBlockTypeface(
                                ResourcesCompat.getFont(context, R.font.jetbrains_mono)!!
                            )
                            .codeBlockTextColor(Color.WHITE)
                            .codeBlockTextSize(spToPx(10f))
                            .codeBlockBackgroundColor(Color.BLACK)
                            .codeTypeface(jetbrainsMonoFont)
                            .codeTextSize(spToPx(10f))
                            .codeTextColor(Color.WHITE)
                            .codeBackgroundColor(Color.BLACK)
                            .isLinkUnderlined(true)
                    }
                }
            )
            .build()

    fun render(mdText: String): Spanned {
        return markwon.render(markwon.parse(mdText))
    }

    private fun spToPx(sp: Float): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, context.resources.displayMetrics)
            .toInt()
}
