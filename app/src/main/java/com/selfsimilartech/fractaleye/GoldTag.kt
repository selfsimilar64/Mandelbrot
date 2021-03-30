package com.selfsimilartech.fractaleye

import android.content.Context
import com.michaelflisar.changelog.tags.IChangelogTag

class GoldTag : IChangelogTag {
    override fun getXMLTagName(): String {
        return "gold"
    }

    override fun formatChangelogRow(context: Context?, changeText: String?): String {
        var prefix = context!!.resources.getString(R.string.changelog_tag_gold_prefix)
        prefix = prefix.replace("\\[".toRegex(), "<").replace("]".toRegex(), ">")
        return prefix + changeText
    }
}