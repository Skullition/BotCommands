package io.github.freya022.botcommands.api.pagination.paginator

import io.github.freya022.botcommands.api.components.utils.ButtonContent
import io.github.freya022.botcommands.api.core.BContext
import io.github.freya022.botcommands.api.pagination.AbstractPaginationBuilder
import io.github.freya022.botcommands.api.pagination.PageEditor
import io.github.freya022.botcommands.api.pagination.Paginators
import io.github.freya022.botcommands.internal.utils.lazyWritable
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder

/**
 * Classic paginator, where each page is generated by a [PageEditor].
 *
 * @see Paginators.paginator
 */
class Paginator internal constructor(
    context: BContext,
    builder: PaginatorBuilder
) : AbstractPaginator<Paginator>(
    context,
    builder
) {
    private val editor: PageEditor<Paginator> = builder.pageEditor

    override var maxPages: Int = builder.maxPages

    init {
        require(maxPages > 0) { "Max pages must be > 0" }
    }

    override fun writeMessage(builder: MessageCreateBuilder) {
        super.writeMessage(builder)

        val embedBuilder = EmbedBuilder()
        editor.accept(this, builder, embedBuilder, page)
        builder.setEmbeds(embedBuilder.build())
    }

    object Defaults {
        /** @see AbstractPaginationBuilder.cleanAfterRefresh */
        @JvmStatic
        var cleanAfterRefresh: Boolean = true

        /** @see PaginatorBuilder.setFirstContent */
        @JvmStatic
        var firstPageButtonContent: ButtonContent by lazyWritable { ButtonContent.fromShortcode(ButtonStyle.PRIMARY, "rewind") }

        /** @see PaginatorBuilder.setPreviousContent */
        @JvmStatic
        var previousPageButtonContent: ButtonContent by lazyWritable { ButtonContent.fromShortcode(ButtonStyle.PRIMARY, "arrow_backward") }

        /** @see PaginatorBuilder.setNextContent */
        @JvmStatic
        var nextPageButtonContent: ButtonContent by lazyWritable { ButtonContent.fromShortcode(ButtonStyle.PRIMARY, "arrow_forward") }

        /** @see PaginatorBuilder.setLastContent */
        @JvmStatic
        var lastPageButtonContent: ButtonContent by lazyWritable { ButtonContent.fromShortcode(ButtonStyle.PRIMARY, "fast_forward") }

        /** @see PaginatorBuilder.setDeleteContent */
        @JvmStatic
        var deleteButtonContent: ButtonContent by lazyWritable { ButtonContent.fromShortcode(ButtonStyle.DANGER, "wastebasket") }

        /** @see PaginatorBuilder.useDeleteButton */
        @JvmStatic
        var useDeleteButton: Boolean = false
    }
}
