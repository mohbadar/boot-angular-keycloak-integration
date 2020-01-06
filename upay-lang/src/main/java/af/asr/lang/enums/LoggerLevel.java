package af.asr.lang.enums;

public enum LoggerLevel {

    TRACE("TRACE"),
    DEBUG("DEBUG"),
    INFO("INFO"),
    WARN("WARN"),
    ERROR("ERROR"),
    FATAL("FATAL"),
    ;

    private String text;

    LoggerLevel(final String text)
    {
        this.text =text;
    }

    @Override
    public String toString() {
        return text;
    }


}
