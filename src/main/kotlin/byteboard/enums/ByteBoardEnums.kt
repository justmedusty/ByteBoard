package byteboard.enums

enum class Length(val value : Long) {
    MAX_CONTENT_LENGTH(10_000),
    MAX_TITLE_LENGTH(300),
    MAX_COMMENT_LENGTH(5000),
    MAX_TOPIC_LENGTH(100)
}
