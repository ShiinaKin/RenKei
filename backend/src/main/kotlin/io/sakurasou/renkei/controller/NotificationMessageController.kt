package io.sakurasou.renkei.controller

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.sakurasou.renkei.model.dao.message.MessageAccessTokenDAO
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory

fun Route.notificationMessageRoutes(messageAccessTokenDAO: MessageAccessTokenDAO) {
    route("notification-message") {
        get {
            call.response.header(HttpHeaders.CacheControl, "no-store")
            call.response.header("Referrer-Policy", "no-referrer")
            call.response.header("X-Robots-Tag", "noindex, nofollow")
            call.response.header(
                "Content-Security-Policy",
                "default-src 'none'; style-src 'unsafe-inline'; script-src 'unsafe-inline'; connect-src 'self'",
            )
            call.respondText(MESSAGE_PAGE, ContentType.Text.Html, HttpStatusCode.OK)
        }

        post("redeem") {
            call.response.header(HttpHeaders.CacheControl, "no-store")
            val token = call.receive<RedeemMessageRequest>().token
            if (!TOKEN_REGEX.matches(token)) {
                logger.info("Rejected malformed notification-message token")
                call.respond(HttpStatusCode.Gone, "The message link is invalid, expired, or already used")
                return@post
            }

            val message = messageAccessTokenDAO.consume(token)
            if (message == null) {
                logger.info("Rejected expired, used, or unknown notification-message token")
                call.respond(HttpStatusCode.Gone, "The message link is invalid, expired, or already used")
                return@post
            }
            logger.info("Redeemed one-time notification-message token: messageId={}", message.messageID)
            call.respond(RedeemMessageResponse(message.messageID, message.content))
        }
    }
}

@Serializable
private data class RedeemMessageRequest(
    val token: String,
)

@Serializable
private data class RedeemMessageResponse(
    val messageID: Long,
    val content: String,
)

private val TOKEN_REGEX = Regex("^[A-Za-z0-9_-]{43}$")
private val logger = LoggerFactory.getLogger("NotificationMessageController")

private const val MESSAGE_PAGE = """<!doctype html>
<html lang="zh-CN">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <meta name="robots" content="noindex,nofollow">
  <title>RenKei 消息</title>
  <style>
    :root { color-scheme: light dark; font-family: -apple-system, BlinkMacSystemFont, sans-serif; }
    body { margin: 0; background: #f3f4f6; color: #171717; }
    main { max-width: 760px; margin: 0 auto; padding: 24px 16px 48px; }
    article { background: #fff; border-radius: 16px; padding: 20px; box-shadow: 0 8px 28px #00000014; }
    h1 { margin: 0 0 16px; font-size: 20px; }
    pre { margin: 0; min-height: 96px; white-space: pre-wrap; overflow-wrap: anywhere; font: 15px/1.55 ui-monospace, SFMono-Regular, Menlo, monospace; }
    button { width: 100%; margin-top: 16px; padding: 13px; border: 0; border-radius: 12px; background: #2563eb; color: white; font-size: 16px; }
    button:disabled { opacity: .55; }
    @media (prefers-color-scheme: dark) { body { background: #111827; color: #f9fafb; } article { background: #1f2937; } }
  </style>
</head>
<body>
  <main><article><h1>RenKei 消息</h1><pre id="content">正在读取…</pre><button id="copy" disabled>复制全文</button></article></main>
  <script>
    const content = document.getElementById('content');
    const copy = document.getElementById('copy');
    const cacheKey = 'renkei-redeemed-message';
    const cached = sessionStorage.getItem(cacheKey);
    const token = location.hash.slice(1);
    history.replaceState(null, '', location.pathname);

    function show(value) {
      content.textContent = value;
      copy.disabled = false;
      copy.onclick = async () => {
        await navigator.clipboard.writeText(value);
        copy.textContent = '已复制';
      };
    }

    if (token) {
      fetch(location.pathname.replace(/\/$/, '') + '/redeem', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({token})
      }).then(async response => {
        if (!response.ok) throw new Error(await response.text());
        return response.json();
      }).then(message => {
        sessionStorage.setItem(cacheKey, message.content);
        show(message.content);
      }).catch(error => { content.textContent = error.message; });
    } else if (cached) {
      show(cached);
    } else {
      content.textContent = '链接无效、已过期或已使用。';
    }
  </script>
</body>
</html>"""
