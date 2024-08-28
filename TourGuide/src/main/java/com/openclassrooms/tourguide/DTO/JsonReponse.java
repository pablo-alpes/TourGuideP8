package com.openclassrooms.tourguide.DTO;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.openclassrooms.tourguide.user.UserExtraInfo;
import gpsUtil.location.Attraction;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;


@Service
public class JsonReponse {
    static final Gson gson = new GsonBuilder().enableComplexMapKeySerialization().setPrettyPrinting()
            .create();
    //  Return a new JSON object that contains: -- All data is contained, needs to wrap into a JSON now
    // Name of Tourist attraction, OK
    // Tourist attractions lat/long, OK
    // The user's location lat/long, OK
    // The distance in miles between the user's location and each of the attractions. OK
    // The reward points for visiting each Attraction. OK
    //    Note: Attraction reward points can be gathered from RewardsCentral


    public String replyJson(Map<Attraction, UserExtraInfo> top5Attractions) throws IOException {
        // Unboxing objects of the keys Technical :: https://stackoverflow.com/questions/8360836/gson-is-there-an-easier-way-to-serialize-a-map
        return gson.toJson(top5Attractions);
        //return top5Attractions.parallelStream().map(gson::toJson).toList();
    }


}
