-keepnames class com.example.data.api.RadioStationResponse
-if class com.example.data.api.RadioStationResponse
-keep class com.example.data.api.RadioStationResponseJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
