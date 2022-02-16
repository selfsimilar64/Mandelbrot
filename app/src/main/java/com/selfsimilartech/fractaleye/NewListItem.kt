package com.selfsimilartech.fractaleye

open class NewListItem<T> (

    val t: T,
    val type: ListItemType

) where T : Customizable {

    override fun equals(other: Any?): Boolean {
        if (other is ListItem<*>) {
            return t == other.t && type == other.type
        }
        return false
    }

    override fun hashCode(): Int {
        return t.hashCode()
    }

}