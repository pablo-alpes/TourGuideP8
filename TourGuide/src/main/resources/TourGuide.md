classDiagram
direction BT
class JsonReponse {
  + JsonReponse() 
  + replyJson(List~UserExtraInfo~) String
}
class RewardsService {
  + RewardsService(GpsUtil, RewardCentral) 
  - int proximityBuffer
  + isWithinAttractionProximity(Attraction, Location) boolean
  + getDistance(Location, Location) double
  + getRewardPoints(Attraction, User) int
  + calculateRewards(User) CompletableFuture~Void~
  + getRewardPointsAsync(Attraction, User) CompletableFuture~Integer~
  + setDefaultProximityBuffer() void
  - nearAttraction(VisitedLocation, Attraction) boolean
  - isLocationMatch(VisitedLocation, Attraction) boolean
   int proximityBuffer
}
class TourGuideController {
  + TourGuideController() 
  + getTripDeals(String) List~Provider~
  + index() String
  - getUser(String) User
  + getLocation(String) VisitedLocation
  + getRewards(String) List~UserReward~
  + getNearbyAttractions(String) CompletableFuture~String~
}
class TourGuideService {
  + TourGuideService(GpsUtil, RewardsService) 
  - generateRandomLongitude() double
  + trackUserLocation(User) CompletableFuture~VisitedLocation~
  + initializeInternalUsers() void
  + getNearByAttractions(VisitedLocation, List~Attraction~, User) List~UserExtraInfo~
  + getTripDeals(User) List~Provider~
  - addShutDownHook() void
  + getUserLocation(User) VisitedLocation
  + addUser(User) void
  + distinctByKey(Function~T, ?~) Predicate~T~
  - generateAttractionLocation() Attraction
  - generateRandomLatitude() double
  - generateUserLocationHistory(User) void
  + getUser(String) User
  + getUserRewards(User) List~UserReward~
   Date randomTime
   List~User~ allUsers
}
class User {
  + User(UUID, String, String, String) 
  - UUID userId
  - String userName
  - UserPreferences userPreferences
  - List~VisitedLocation~ visitedLocations
  - List~Provider~ tripDeals
  - String phoneNumber
  - Date latestLocationTimestamp
  - String emailAddress
  - List~UserReward~ userRewards
  + clearVisitedLocations() void
  + addToVisitedLocations(VisitedLocation) void
  + addUserReward(UserReward) void
   Date latestLocationTimestamp
   String emailAddress
   List~UserReward~ userRewards
   String userName
   String phoneNumber
   UUID userId
   List~VisitedLocation~ visitedLocations
   List~Provider~ tripDeals
   VisitedLocation lastVisitedLocation
   UserPreferences userPreferences
}
class UserExtraInfo {
  + UserExtraInfo(Attraction, double, int, double, double) 
  - double userLongitude
  - double distance
  - double userLatitude
  - int rewardPoints
   double userLatitude
   double userLongitude
   double distance
   int rewardPoints
}
class UserPreferences {
  + UserPreferences() 
  - int numberOfAdults
  - int numberOfChildren
  - int tripDuration
  - int ticketQuantity
  - int attractionProximity
   int ticketQuantity
   int numberOfAdults
   int numberOfChildren
   int tripDuration
   int attractionProximity
}
class UserReward {
  + UserReward(VisitedLocation, Attraction, int) 
  + UserReward(VisitedLocation, Attraction) 
  - int rewardPoints
   int rewardPoints
}
class Attraction {
    + Attraction(String attractionName, String city, String state, double latitude, double longitude)
    + String attractionName
    + String city
    + String state
    + String UUID attractionId 
}
class VisitedLocation {
    + VisitedLocation(UUID userId, Location location, Date timeVisited)
    + UUID userId
    + Location location
    + Date timeVisited
}


TourGuideController "1" *--> "jsonReponse 1" JsonReponse 
TourGuideController "1" *--> "rewardsService 1" RewardsService 
TourGuideController "1" *--> "tourGuideService 1" TourGuideService 
TourGuideService "1" *--> "rewardsService 1" RewardsService 
TourGuideService "1" *--> "User *" User 
User "1" *--> "userPreferences 1" UserPreferences 
User "1" *--> "userRewards *" UserReward 
User "1" *--> "userRewards *" UserReward 
Attraction "1" *--> "userRewards 1" UserReward
Attraction "1" *--> "UserExtraInfo 1" UserExtraInfo  
VisitedLocation "1" *--> "userRewards 1" UserReward
VisitedLocation "1" *--> "User 1" User
UserExtraInfo "*" *--> "jsonReponse 1" JsonReponse 


