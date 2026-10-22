rootProject.name = "ticketing-platform"

include(
    "libs:platform-commons",
    "services:gateway",
    "services:catalog",
    "services:reservation",
    "services:payment",
    "services:notification",
)
