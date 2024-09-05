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

    public String replyJson(List<UserExtraInfo> top5Attractions) throws IOException {
        // Unboxing objects of the keys Technical :: https://stackoverflow.com/questions/8360836/gson-is-there-an-easier-way-to-serialize-a-map
        return gson.toJson(top5Attractions);
    }


}
