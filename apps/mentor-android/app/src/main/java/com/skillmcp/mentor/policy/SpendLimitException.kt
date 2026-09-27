package com.skillmcp.mentor.policy

class SpendLimitException(val check: AllowanceCheck) : IllegalStateException(check.message)
