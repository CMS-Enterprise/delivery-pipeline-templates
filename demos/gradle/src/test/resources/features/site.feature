Feature: RoboCare Health maintenance portal
  Visitors and monitoring probes reach the public pages of the site.

  Scenario: Health probe reports the service is up
    When a client requests "/health"
    Then the response status is 200
    And the response body contains "\"status\":\"ok\""

  Scenario Outline: Public pages render the RoboCare Health brand
    When a client requests "<path>"
    Then the response status is 200
    And the response body contains "<text>"

    Examples:
      | path   | text                  |
      | /      | RoboCare Health       |
      | /about | About RoboCare Health |

  Scenario: Unknown pages are not served
    When a client requests "/no-such-page"
    Then the response status is 404
