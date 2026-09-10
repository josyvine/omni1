package com.vineyard.omnicam.app.core.constants

object ApiEndpoints {
    const val ONVIF_MULTICAST_IP = "239.255.255.250"
    const val ONVIF_WS_DISCOVERY_PORT = 3702
    const val DEFAULT_RTSP_PORT = 554
    const val DEFAULT_ONVIF_PORT = 2020

    const val GOOGLE_OAUTH_AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth"
    const val GOOGLE_OAUTH_TOKEN_URL = "https://oauth2.googleapis.com/token"
    const val GOOGLE_DRIVE_API_BASE = "https://www.googleapis.com/drive/v3"
    const val GOOGLE_DRIVE_UPLOAD_BASE = "https://www.googleapis.com/upload/drive/v3"
    const val GOOGLE_DRIVE_SCOPE = "https://www.googleapis.com/auth/drive.file"

    const val OAUTH_REDIRECT_SCHEME = "com.vineyard.omnicam.app"
    const val OAUTH_REDIRECT_HOST = "oauth2redirect"
    const val OAUTH_REDIRECT_URI = "$OAUTH_REDIRECT_SCHEME://$OAUTH_REDIRECT_HOST"

    const val FIRESTORE_COLLECTION_CAMERAS = "cameras"
    const val FIRESTORE_COLLECTION_USERS = "users"
    const val FIRESTORE_COLLECTION_SHARED = "shared_access"
    const val FIRESTORE_COLLECTION_PROVIDERS = "camera_providers"
    const val DRIVE_FOLDER_NAME = "OmniCam_Recordings"
}
