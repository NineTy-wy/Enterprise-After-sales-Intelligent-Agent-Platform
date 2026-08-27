from dataclasses import dataclass
from typing import Any

import httpx

from .config import Settings


@dataclass(frozen=True)
class ToolSpec:
    name: str
    description: str
    required_arguments: tuple[str, ...]


class ToolRegistry:
    """Agent 可调用工具白名单，避免模型自由拼接任意 HTTP 请求。"""

    def __init__(self, settings: Settings) -> None:
        self.settings = settings
        self.specs = {
            "create_ticket": ToolSpec(
                "create_ticket",
                "创建售后工单",
                ("customerName", "issueDescription"),
            ),
            "get_ticket": ToolSpec(
                "get_ticket",
                "查询售后工单",
                ("ticketId",),
            ),
        }

    async def invoke(
        self,
        name: str,
        arguments: dict[str, Any],
        authorization: str | None = None,
    ) -> dict[str, Any]:
        spec = self.specs.get(name)
        if spec is None:
            raise ValueError("tool is not allow-listed")
        missing = [key for key in spec.required_arguments if not arguments.get(key)]
        if missing:
            raise ValueError(f"missing tool arguments: {','.join(missing)}")

        headers = {}
        if authorization:
            headers["Authorization"] = authorization
        async with httpx.AsyncClient(timeout=20) as client:
            response = await client.post(
                f"{self.settings.backend_service_url.rstrip('/')}/api/tools/{name}/invoke",
                headers=headers,
                json={"arguments": arguments},
            )
            response.raise_for_status()
            return response.json()


def should_create_ticket(query: str) -> bool:
    markers = ("创建工单", "新建工单", "开工单", "登记故障")
    return any(marker in query for marker in markers)
