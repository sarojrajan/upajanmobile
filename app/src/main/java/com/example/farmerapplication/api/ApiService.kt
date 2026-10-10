package com.example.farmerapplication.api

import com.example.farmerapplication.models.AadhaarOtpResponse
import com.example.farmerapplication.models.AadhaarSendOtpRequest
import com.example.farmerapplication.models.AadhaarVerifyOtpRequest
import com.example.farmerapplication.models.BankDetailsResponse
import com.example.farmerapplication.models.BankResponse
import com.example.farmerapplication.models.BlockResponse
import com.example.farmerapplication.models.CircleResponse
import com.example.farmerapplication.models.DistrictResponse
import com.example.farmerapplication.models.FarmerFullRegistrationRequest
import com.example.farmerapplication.models.FarmerFullRegistrationResponse
import com.example.farmerapplication.models.FarmerRegistrationRequest
import com.example.farmerapplication.models.FarmerRegistrationResponse
import com.example.farmerapplication.models.FarmerSlotBookingRequest
import com.example.farmerapplication.models.FarmerSlotBookingResponse
import com.example.farmerapplication.models.GenerateOtpRequest
import com.example.farmerapplication.models.GenerateOtpResponse
import com.example.farmerapplication.models.HalkaResponse
import com.example.farmerapplication.models.MSPLoginRequest
import com.example.farmerapplication.models.MSPLoginResponse
import com.example.farmerapplication.models.MaujaResponse
import com.example.farmerapplication.models.PanchayatResponse
import com.example.farmerapplication.models.SaveLandRecordsRequest
import com.example.farmerapplication.models.SaveLandRecordsResponse
import com.example.farmerapplication.models.VerifyOtpRequest
import com.example.farmerapplication.models.VerifyOtpResponse
import com.example.farmerapplication.models.VillageResponse
import com.example.farmerapplication.models.RegistrationAvailabilityResponse
import com.example.farmerapplication.models.MSPCenterDetailsResponse
import com.example.farmerapplication.models.FarmerProfileDetailsResponse
import com.example.farmerapplication.models.FarmerLandDetailsResponse
import com.example.farmerapplication.models.FarmerEligibilityResponse
import com.example.farmerapplication.models.SavePaddyLiftRecordsRequest
import com.example.farmerapplication.models.SavePaddyLiftRecordsResponse
import com.example.farmerapplication.models.MillerLoginRequest
import com.example.farmerapplication.models.MillerLoginResponse
import com.example.farmerapplication.models.ScheduledFarmerResponse
import com.example.farmerapplication.models.CommodityReceivedResponse
import com.example.farmerapplication.models.MillerDistrictItem
import com.example.farmerapplication.models.MillerJsfcGodownItem
import com.example.farmerapplication.models.CommodityTypeItem
import com.example.farmerapplication.models.VehicleTypeItem
import com.example.farmerapplication.models.GunnyBagYearItem
import com.example.farmerapplication.models.GunnyBagTypeItem
import com.example.farmerapplication.models.RiceSubmitRequest
import com.example.farmerapplication.models.RiceSubmitResponse
import com.example.farmerapplication.models.FarmerDetailsResponse
import com.example.farmerapplication.models.SendFarmerSmsRequest
import com.example.farmerapplication.models.SendFarmerSmsResponse
import com.example.farmerapplication.models.DSOLoginRequest
import com.example.farmerapplication.models.DSOLoginResponse
import com.example.farmerapplication.models.PaddyCapacityResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query
import okhttp3.ResponseBody

interface ApiService {

    /** Fetch all districts. */
    @Headers("Accept: application/json", "X-Tunnel-Skip-Anti-Phishing-Page: true")
    @GET("api/NecessaryDropDown/GetAllDistricts")
    fun getDistricts(): Call<DistrictResponse>

    /** Generate OTP for farmer login. */
    @Headers("Accept: application/json", "Content-Type: application/json", "X-Tunnel-Skip-Anti-Phishing-Page: true")
    @POST("api/Farmer/GenerateOTP")
    fun generateOtp(@Body request: GenerateOtpRequest): Call<GenerateOtpResponse>

    /** Verify OTP for farmer login. */
    @Headers("Accept: application/json", "Content-Type: application/json", "X-Tunnel-Skip-Anti-Phishing-Page: true")
    @POST("api/Farmer/verify-otp")
    fun verifyOtp(@Body request: VerifyOtpRequest): Call<VerifyOtpResponse>

    /** Generate Aadhaar-based OTP. */
    @Headers("Accept: application/json", "Content-Type: application/json", "X-Tunnel-Skip-Anti-Phishing-Page: true")
    @POST("api/AadharAuthenification/send-otp")
    fun sendAadhaarOtp(@Body request: AadhaarSendOtpRequest): Call<AadhaarOtpResponse>

    /** Verify Aadhaar-based OTP. */
    @Headers("Accept: application/json", "Content-Type: application/json", "X-Tunnel-Skip-Anti-Phishing-Page: true")
    @POST("api/AadharAuthenification/verify-otp")
    fun verifyAadhaarOtp(@Body request: AadhaarVerifyOtpRequest): Call<AadhaarOtpResponse>

    /** Farmer self-registration. */
    @Headers("Accept: application/json", "Content-Type: application/json", "X-Tunnel-Skip-Anti-Phishing-Page: true")
    @POST("api/Farmer/self-registration")
    fun registerFarmer(@Body request: FarmerRegistrationRequest): Call<FarmerRegistrationResponse>

    /** Fetch blocks/subdistricts using district code. */
    @Headers("Accept: application/json", "X-Tunnel-Skip-Anti-Phishing-Page: true")
    @GET("api/NecessaryDropDown/GetBlocks")
    fun getBlocks(@Query("district_code") districtCode: String): Call<BlockResponse>

    /** Fetch panchayats using block/subdistrict code. */
    @Headers("Accept: application/json", "X-Tunnel-Skip-Anti-Phishing-Page: true")
    @GET("api/NecessaryDropDown/GetPanchayats")
    fun getPanchayats(@Query("subdistrict_code") subdistrictCode: String): Call<PanchayatResponse>

    /** Fetch villages using panchayat/local body code. */
    @Headers("Accept: application/json", "X-Tunnel-Skip-Anti-Phishing-Page: true")
    @GET("api/NecessaryDropDown/GetVillages")
    fun getVillages(@Query("local_body_code") localBodyCode: String): Call<VillageResponse>

    /** Fetch banks. */
    @Headers("Accept: application/json", "X-Tunnel-Skip-Anti-Phishing-Page: true")
    @GET("api/NecessaryDropDown/GetBanks")
    fun getBanks(): Call<BankResponse>

    /** Fetch bank branches using bank ID. */
    @Headers("Accept: application/json", "X-Tunnel-Skip-Anti-Phishing-Page: true")
    @GET("api/NecessaryDropDown/GetBankDetails")
    fun getBankDetails(@Query("bank_id") bankId: String): Call<BankDetailsResponse>

    /** Fetch circles using district code. */
    @Headers("Accept: application/json", "X-Tunnel-Skip-Anti-Phishing-Page: true")
    @GET("api/NecessaryDropDown/GetCircles")
    fun getCircles(@Query("district_code") districtCode: String): Call<CircleResponse>

    /** Fetch Halkas using district and circle codes. */
    @Headers("Accept: application/json", "X-Tunnel-Skip-Anti-Phishing-Page: true")
    @GET("api/NecessaryDropDown/GetHalkas")
    fun getHalkas(@Query("district_code") districtCode: String, @Query("circle_code") circleCode: String): Call<HalkaResponse>

    /** Fetch Maujas using district, circle, and Halka codes. */
    @Headers("Accept: application/json", "X-Tunnel-Skip-Anti-Phishing-Page: true")
    @GET("api/NecessaryDropDown/GetMaujas")
    fun getMaujas(@Query("district_code") districtCode: String, @Query("circle_code") circleCode: String, @Query("halka_code") halkaCode: String): Call<MaujaResponse>

    /** Farmer full registration. */
    @Headers("Accept: application/json", "Content-Type: application/json", "X-Tunnel-Skip-Anti-Phishing-Page: true")
    @POST("api/Farmer/full-registration")
    fun submitFullRegistration(@Body request: FarmerFullRegistrationRequest): Call<FarmerFullRegistrationResponse>

    /** Save land records. */
    @Headers("Accept: application/json; charset=utf-8", "Content-Type: application/json; charset=utf-8", "X-Tunnel-Skip-Anti-Phishing-Page: true")
    @POST("api/Farmer/save-land-records")
    fun saveLandRecords(@Body request: SaveLandRecordsRequest): Call<SaveLandRecordsResponse>

    /** Save farmer slot-booking details. */
    @Headers("Accept: application/json", "Content-Type: application/json", "X-Tunnel-Skip-Anti-Phishing-Page: true")
    @POST("api/Farmer/save-farmer-sms-details")
    fun saveFarmerSlotBooking(@Body request: FarmerSlotBookingRequest): Call<FarmerSlotBookingResponse>

    /** MSP Centre login endpoint. */
    @Headers("Accept: application/json", "Content-Type: application/json", "X-Tunnel-Skip-Anti-Phishing-Page: true")
    @POST("api/MSPCenter/login")
    fun loginMSP(@Body request: MSPLoginRequest): Call<MSPLoginResponse>

    /** Check whether mobile number is already registered. */
    @Headers("Accept: application/json", "X-Tunnel-Skip-Anti-Phishing-Page: true")
    @GET("api/Farmer/check-mobile")
    fun checkMobileNumber(@Query("mobileNo") mobileNo: String): Call<RegistrationAvailabilityResponse>

    /** Check whether Aadhaar number is already registered. */
    @Headers("Accept: application/json", "X-Tunnel-Skip-Anti-Phishing-Page: true")
    @GET("api/Farmer/check-aadhaar")
    fun checkAadhaarNumber(@Query("aadhaarNo") aadhaarNo: String): Call<RegistrationAvailabilityResponse>

    /** Update the isAadhaarVerified column for aadhaar verified registered farmers **/
    @Headers("Accept: application/json", "X-Tunnel-Skip-Anti-Phishing-Page: true")
    @POST("api/Farmer/update-aadhaar-verification")
    fun updateAadhaarVerification(@Query("aadhaarNo") aadhaarNo: String): Call<ResponseBody>

    /** Fetch MSP Centre name using MSP Centre ID. */
    @Headers("Accept: application/json", "X-Tunnel-Skip-Anti-Phishing-Page: true")
    @GET("api/MSPCenter/get-msp-center-by-id")
    fun getMSPCenterById(@Query("mspCenterId") mspCenterId: String): Call<MSPCenterDetailsResponse>

    /** Fetch farmer profile details by Farmer ID. */
    @GET("api/Farmer/GetFarmerProfileDetails")
    fun getFarmerProfileDetails(@Query("farmerId") farmerId: String): Call<FarmerProfileDetailsResponse>

    /** Fetch farmer land details by Farmer ID. */
    @GET("api/Farmer/GetFarmerLandDetails")
    fun getFarmerLandDetails(@Query("farmer_id") farmerId: String): Call<FarmerLandDetailsResponse>

    // Check farmer eligibility for slot booking
    @Headers("Accept: application/json", "X-Tunnel-Skip-Anti-Phishing-Page: true")
    @GET("api/Farmer/CheckFarmerEligibility")
    fun checkFarmerEligibility(@Query("farmer_id") farmerId: String, @Query("district_id") districtId: String, @Query("mspcentre_id") mspCenterId: String): Call<FarmerEligibilityResponse>

    // Save Paddy Lift RO and vehicle-wise records
    @Headers("Accept: application/json", "Content-Type: application/json", "X-Tunnel-Skip-Anti-Phishing-Page: true")
    @POST("api/MSPCenter/SavePaddyLiftRecords")
    fun savePaddyLiftRecords(@Body request: SavePaddyLiftRecordsRequest): Call<SavePaddyLiftRecordsResponse>

    // Miller Login
    @Headers("Accept: application/json", "Content-Type: application/json", "X-Tunnel-Skip-Anti-Phishing-Page: true")
    @POST("api/Miller/login")
    fun millerLogin(@Body request: MillerLoginRequest): Call<MillerLoginResponse>

    /** Today's scheduled farmers for an MSP Centre. */
    @Headers("Accept: application/json", "X-Tunnel-Skip-Anti-Phishing-Page: true")
    @GET("api/MSPCenter/today-scheduled-farmers")
    fun getTodayScheduledFarmers(@Query("mspCenterPlaceName") mspCenterPlaceName: String): Call<ScheduledFarmerResponse>

    /** Commodity (paddy) received farmer details for an MSP Centre. */
    @Headers("Accept: application/json", "X-Tunnel-Skip-Anti-Phishing-Page: true")
    @GET("api/MSPCenter/commodity-received")
    fun getCommodityReceived(@Query("societyId") societyId: String): Call<CommodityReceivedResponse>


    /** Fetch districts for a given miller. */
    @Headers("Accept: application/json", "X-Tunnel-Skip-Anti-Phishing-Page: true")
    @GET("api/Miller/districts")
    fun getMillerDistricts(@Query("millerId") millerId: Int): Call<List<MillerDistrictItem>>

    /** Fetch JSFC godowns for a given district. */
    @Headers("Accept: application/json", "X-Tunnel-Skip-Anti-Phishing-Page: true")
    @GET("api/Miller/jsfc-godowns")
    fun getJsfcGodowns(@Query("districtCode") districtCode: String): Call<List<MillerJsfcGodownItem>>

    /** Fetch commodity types. */
    @Headers("Accept: application/json", "X-Tunnel-Skip-Anti-Phishing-Page: true")
    @GET("api/Miller/commodity-types")
    fun getCommodityTypes(): Call<List<CommodityTypeItem>>

    /** Fetch vehicle types. */
    @Headers("Accept: application/json", "X-Tunnel-Skip-Anti-Phishing-Page: true")
    @GET("api/Miller/vehicle-types")
    fun getVehicleTypes(): Call<List<VehicleTypeItem>>

    /** Fetch gunny bag years. */
    @Headers("Accept: application/json", "X-Tunnel-Skip-Anti-Phishing-Page: true")
    @GET("api/Miller/gunny-bag-years")
    fun getGunnyBagYears(): Call<List<GunnyBagYearItem>>

    /** Fetch gunny bag types. */
    @Headers("Accept: application/json", "X-Tunnel-Skip-Anti-Phishing-Page: true")
    @GET("api/Miller/gunny-bag-types")
    fun getGunnyBagTypes(): Call<List<GunnyBagTypeItem>>

    /** Submit Advance CMR / rice submission. */
    @Headers("Accept: application/json", "Content-Type: application/json", "X-Tunnel-Skip-Anti-Phishing-Page: true")
    @POST("api/Miller/rice-submit")
    fun submitRiceAdvanceCMR(@Body request: RiceSubmitRequest): Call<RiceSubmitResponse>

    /** MSP: fetch farmer details for current slot booking. */
    @Headers("Accept: application/json", "X-Tunnel-Skip-Anti-Phishing-Page: true")
    @GET("api/MSPCenter/farmer-details")
    fun getFarmerDetails(
        @Query("FarmerId") farmerId: String,
        @Query("DistrictId") districtId: String,
        @Query("SocId") socId: String
    ): Call<FarmerDetailsResponse>

    /** MSP: send SMS to farmer. */
    @Headers("Accept: application/json", "Content-Type: application/json", "X-Tunnel-Skip-Anti-Phishing-Page: true")
    @POST("api/MSPCenter/send-farmer-sms")
    fun sendFarmerSms(@Body request: SendFarmerSmsRequest): Call<SendFarmerSmsResponse>

    /** DSO login. */
    @Headers("Accept: application/json", "X-Tunnel-Skip-Anti-Phishing-Page: true")
    @POST("api/Auth/DSO-DMSFCLogin")
    fun dsoLogin(@Body request: DSOLoginRequest): Call<DSOLoginResponse>

    /** DMSFC login (same endpoint as DSO, usertype_id = 6). */
    @Headers("Accept: application/json", "X-Tunnel-Skip-Anti-Phishing-Page: true")
    @POST("api/Auth/DSO-DMSFCLogin")
    fun dmsfcLogin(@Body request: DSOLoginRequest): Call<DSOLoginResponse>

    /** Fetch paddy capacity details for an MSP centre (PACS). */
    @Headers("Accept: application/json", "X-Tunnel-Skip-Anti-Phishing-Page: true")
    @GET("api/MSPCenter/paddy-capacity")
    fun getPaddyCapacity(@Query("pacsId") pacsId: String): Call<PaddyCapacityResponse>


}