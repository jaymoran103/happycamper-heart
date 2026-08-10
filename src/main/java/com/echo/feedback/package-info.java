/**
 * Local-only feedback reporting: builds a diagnostic summary the user can review and send.
 *
 * Nothing in this package transmits data. The only network actor is the user's own browser,
 * opened at a prefilled WhatsApp link they must still press send on. Roster rows never enter
 * a report — see WarningSummarizer for the rules that keep camper names out.
 */
package com.echo.feedback;
