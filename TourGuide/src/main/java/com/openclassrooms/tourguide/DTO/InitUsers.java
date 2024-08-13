package com.openclassrooms.tourguide.DTO;

import com.openclassrooms.tourguide.config.Constants;
import com.openclassrooms.tourguide.helper.InternalTestHelper;
import com.openclassrooms.tourguide.service.RewardsService;
import com.openclassrooms.tourguide.service.TourGuideService;
import gpsUtil.GpsUtil;
import rewardCentral.RewardCentral;

public class InitUsers {

    public static void initializeMockUsers() {
        InternalTestHelper.setInternalUserNumber(Constants.USERSINIT);

        GpsUtil gpsUtil = new GpsUtil();
        RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());
        TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);

        tourGuideService.initializeInternalUsers();
    }
}
