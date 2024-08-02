package com.openclassrooms.tourguide.user;

/**
 * This model represents the extra information requested for the json on top of the standard reply for the top 5 destinations most close to the user
 * the user latest position is going to be repeated as it takes the latest one since the qyery is done
 */

public class UserExtraInfo {
    private double distance;
    private int rewardPoints;
    private double userLongitude;
    private double userLatitude;

    public UserExtraInfo() {
    }

    public UserExtraInfo(double distance, int rewardPoints, double userLongitude, double userLatitude) {
        this.distance = distance;
        this.rewardPoints = rewardPoints;
        this.userLongitude = userLongitude;
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