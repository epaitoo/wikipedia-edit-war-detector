package com.epaitoo.springboot.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class WikimediaEditEvent {
    // Direct fields from JSON
    @JsonProperty("title")
    private String pageTitle;

    @JsonProperty("user")
    private String username;

    @JsonProperty("timestamp")
    private Long timestamp;

    @JsonProperty("bot")
    private Boolean isBot;

    @JsonProperty("namespace")
    private Integer namespace;

    @JsonProperty("type")
    private String type;

    // Nested fields
    @JsonProperty("length")
    private LengthInfo length;

    @JsonProperty("meta")
    private MetaInfo meta;

    // Computed/helper fields (not from JSON)
    private Integer lengthOld;
    private Integer lengthNew;
    private String wiki;

    // Inner classes for nested JSON structures
    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LengthInfo {
        @JsonProperty("old")
        private Integer old;

        @JsonProperty("new")
        private Integer newLength;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MetaInfo {
        @JsonProperty("domain")
        private String domain;

        @JsonProperty("uri")
        private String uri;

        @JsonProperty("dt")
        private String dt;
    }

    // Post-processing: flatten nested fields
    public void processNestedFields() {
        if (length != null) {
            this.lengthOld = length.getOld();
            this.lengthNew = length.getNewLength();
        }

        if (meta != null) {
            this.wiki = meta.getDomain();
        }

        // Set defaults
        if (isBot == null) isBot = false;
        if (namespace == null) namespace = -1;
        if (wiki == null) wiki = "unknown";
    }

    // Helper methods
    public Integer getLengthChange() {
        if (lengthNew == null || lengthOld == null) {
            return 0;
        }
        return lengthNew - lengthOld;
    }

    public boolean isMainNamespace() {
        return namespace != null && namespace == 0;
    }

    public boolean isHumanEdit() {
        return isBot != null && !isBot;
    }

    public boolean isEditType() {
        return "edit".equals(type);
    }




}
