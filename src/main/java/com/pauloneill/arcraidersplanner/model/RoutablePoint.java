package com.pauloneill.arcraidersplanner.model;

/**
 * Interface for any entity that can be a point in a route.
 * WHY: Provides a common abstraction for Area and MapMarker to be used in routing algorithms.
 */
public interface RoutablePoint {
    String getId();
    double getX();
    double getY();
    String getName();
}