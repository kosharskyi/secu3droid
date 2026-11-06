## MainActivity owns global message display, not logger error production

MainActivity consumes the singleton global message queue while RESUMED. SecuLogger publishes its localized logger error directly after LoggerSession has cleared the failed output; Activity does not inject or control the logger. Keep toast display and queue pacing in the Activity.
