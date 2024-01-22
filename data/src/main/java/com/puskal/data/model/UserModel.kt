package com.puskal.data.model

import com.puskal.core.extension.formattedCount
import com.google.gson.annotations.SerializedName

/**
 * Created by Puskal Khadka on 3/18/2023.
 */
data class UserModel(
    val userId: Long = 12333,
    val uniqueUserName: String = "PETEROTTO123",
    val fullName: String = "PETER OTTO",
    val following: Long = 123333,
    val followers: Long = 1233,
    //@SerializedName("likes")
    val likes: Long = 333,
    val bio: String = "lelo",
    val profilePic: String = "lel",
    val isVerified: Boolean = false,
    val isLikedVideoPrivate: Boolean = true,
    val pinSocialMedia: SocialMedia? = null,
    var isFollowed: Boolean = true,
) {
    var formattedFollowingCount: String = ""
    var formattedFollowersCount: String = ""
    var formattedLikeCount: String = ""

    fun fix() {
        formattedLikeCount = likes.formattedCount()
        formattedFollowingCount = following.formattedCount()
        formattedFollowersCount = followers.formattedCount()
    }
    init {
        formattedLikeCount = likes.formattedCount()
        formattedFollowingCount = following.formattedCount()
        formattedFollowersCount = followers.formattedCount()
    }

    data class SocialMedia(
        val type: SocialMediaType,
        val link: String
    )
}

enum class SocialMediaType {
    INSTAGRAM,
    YOUTUBE
}