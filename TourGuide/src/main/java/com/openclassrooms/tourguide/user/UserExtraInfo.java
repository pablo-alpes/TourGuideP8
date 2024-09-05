package com.openclassrooms.tourguide.user;

import gpsUtil.location.Attraction;

import java.util.UUID;

/**
 * This model represents the extra information requested for the json on top of the standard reply for the top 5 destinations most close to the user
 * the user latest position is going to be repeated as it takes the latest one since the qyery is done
 */

public class UserExtraInfo extends Attraction {

    private double distance;
    private int rewardPoints;
    private double userLongitude;
    private double userLatitude;

    public UserExtraInfo(Attraction attraction, double distance, int rewardPoints, double userLongitude, double userLatitude) {
        super(attraction.attractionName,
                attraction.city,
                attraction.state,
                attraction.latitude,
                attraction.longitude);
        this.distance = distance;
        this.rewardPoints = rewardPoints;
        this.userLongitude = userLongitude;
        this.userLatitude = userLatitude;
    }


    public void setDistance(double distance) {
        this.distance = distance;
    }

    public void setRewardPoints(int rewardPoints) {
        this.rewardPoints = rewardPoints;
    }

    public void setUserLongitude(double userLongitude) {
        this.userLongitude = userLongitude;
    }

    public void setUserLatitude(double userLatitude) {
        this.userLatitude = userLatitude;
    }

    public double getDistance() {
        return distance;
    }

    public int getRewardPoints() {
        return rewardPoints;
    }

    public double getUserLongitude() {
        return userLongitude;
    }

    public double getUserLatitude() {
        return userLatitude;
    }
}