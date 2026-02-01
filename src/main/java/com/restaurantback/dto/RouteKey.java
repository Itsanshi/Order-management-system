package com.restaurantback.dto;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RouteKey {

    private final String method;

    private final String path;

    private final Pattern pathPattern;


    public RouteKey(String method, String path) {

        this.method = method;

        this.path = path;

        // Convert {id} to a regex matching dynamic segments

        this.pathPattern = Pattern.compile(path.replaceAll("\\{[^/]+}", "[^/]+")); // Matches /locations/{id}/feedbacks

    }

    public String getMethod() {

        return method;

    }

    public String getPath() {

        return path;

    }

    public boolean matches(String method, String incomingPath) {

        return this.method.equalsIgnoreCase(method) && pathPattern.matcher(incomingPath).matches();

    }


    @Override

    public boolean equals(Object o) {

        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        RouteKey routeKey = (RouteKey) o;

        return Objects.equals(method, routeKey.method) &&

                Objects.equals(path, routeKey.path);

    }

    @Override

    public int hashCode() {

        return Objects.hash(method, path);

    }

    @Override

    public String toString() {

        return "RouteKey{" +

                "method='" + method + '\'' +

                ", path='" + path + '\'' +

                '}';

    }

    public Map<String, String> extractPathVariables(String incomingPath) {

        Map<String, String> pathVariables = new HashMap<>();

        Matcher matcher = pathPattern.matcher(incomingPath);

        if (matcher.matches()) {

            // Split the predefined template path and incoming path into segments

            String[] templateParts = path.split("/");

            String[] actualParts = incomingPath.split("/");

            // Extract variables where placeholders exist (e.g., {id})

            for (int i = 0; i < templateParts.length; i++) {

                if (templateParts[i].startsWith("{") && templateParts[i].endsWith("}")) {

                    String paramName = templateParts[i].substring(1, templateParts[i].length() - 1); // Strip { }

                    pathVariables.put(paramName, actualParts[i]); // Map paramName -> value

                }

            }

        }

        return pathVariables;

    }

}

