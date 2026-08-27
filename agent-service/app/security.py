from fastapi import Header, HTTPException

from .config import get_settings


async def verify_service_token(
    authorization: str | None = Header(default=None),
) -> None:
    expected = get_settings().agent_service_token
    if not expected:
        return
    if authorization != f"Bearer {expected}":
        raise HTTPException(status_code=401, detail="invalid service token")
