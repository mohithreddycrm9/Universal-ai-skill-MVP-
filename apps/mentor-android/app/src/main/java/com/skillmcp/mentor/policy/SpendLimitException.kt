package com.skillmcp.mentor.policy

class SpendLimitException(val check: SpendCheck) : IllegalStateException(check.message)
