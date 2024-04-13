package byteboard.enums

enum class Length(val value : Long) {
    MAX_CONTENT_LENGTH(10_000),
    MAX_TITLE_LENGTH(300),
    MAX_COMMENT_LENGTH(5_000),
    MAX_TOPIC_LENGTH(100),
    MAX_PHOTO_SIZE_BYTES(1_048_576),
    MAX_DM_MESSAGE_LENGTH(5_000),
    MAX_LIMIT(250)
}
