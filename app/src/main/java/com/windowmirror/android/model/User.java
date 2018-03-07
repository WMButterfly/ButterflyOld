package com.windowmirror.android.model;

public class User {
    private String email;
    private String name;
    private String pictureURL;
    private String grantedScope;
    private String message;

    private User() {
        // Only accessible via Builder
    }

    public String getEmail() {
        return email;
    }

    public String getGrantedScope() {
        return grantedScope;
    }

    public String getName() {
        return name;
    }

    public String getPictureURL() {
        return pictureURL;
    }

    public String getMessage() {
        return message;
    }

    public Boolean hasScope(String scope) {
        return grantedScope.contains(scope);
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public static class Builder {
        private String email;
        private String name;
        private String pictureURL;
        private String grantedScope;
        private String message;

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder pictureUrl(String url) {
            this.pictureURL = url;
            return this;
        }

        public Builder grantedScope(String grantedScope) {
            this.grantedScope = grantedScope;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public User build() {
            User user = new User();
            user.email = email;
            user.name = name;
            user.pictureURL = pictureURL;
            user.grantedScope = grantedScope;
            user.message = message;
            return user;
        }
    }
}
