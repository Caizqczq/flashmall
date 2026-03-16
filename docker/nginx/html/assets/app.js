const lbModeSelect = document.getElementById("lbMode");
const refreshBtn = document.getElementById("refreshBtn");
const goodsList = document.getElementById("goodsList");
const listStatus = document.getElementById("listStatus");
const detailStatus = document.getElementById("detailStatus");
const detailResult = document.getElementById("detailResult");
const backendInstance = document.getElementById("backendInstance");
const nginxUpstream = document.getElementById("nginxUpstream");

const basePaths = {
    roundRobin: "/api/goods",
    leastConn: "/api/lb/least/goods",
    ipHash: "/api/lb/iphash/goods",
    uriHash: "/api/lb/urihash/goods"
};

async function requestJson(path) {
    const response = await fetch(path, {
        headers: {
            "Accept": "application/json"
        }
    });
    const data = await response.json();
    return {
        data,
        backendInstance: response.headers.get("X-Backend-Instance") || "-",
        nginxUpstream: response.headers.get("X-Nginx-Upstream") || "-"
    };
}

function getBasePath() {
    return basePaths[lbModeSelect.value];
}

function renderGoodsList(items) {
    goodsList.innerHTML = "";
    items.forEach((item) => {
        const card = document.createElement("article");
        card.className = "goods-card";
        card.innerHTML = `
            <h3>${item.goodsName}</h3>
            <p>${item.goodsDetail || "暂无描述"}</p>
            <div class="price">¥${item.goodsPrice}</div>
            <button data-id="${item.id}">查看详情</button>
        `;
        card.querySelector("button").addEventListener("click", () => loadDetail(item.id));
        goodsList.appendChild(card);
    });
}

async function loadList() {
    listStatus.textContent = "加载中";
    try {
        const result = await requestJson(`${getBasePath()}/list?pageNum=1&pageSize=6`);
        const records = result.data?.data?.records || [];
        renderGoodsList(records);
        listStatus.textContent = `已加载 ${records.length} 个商品`;
        if (records.length > 0) {
            loadDetail(records[0].id);
        }
    } catch (error) {
        listStatus.textContent = "加载失败";
        goodsList.innerHTML = `<div class="goods-card"><p>${error.message}</p></div>`;
    }
}

async function loadDetail(id) {
    detailStatus.textContent = `加载商品 ${id}`;
    try {
        const result = await requestJson(`${getBasePath()}/detail/${id}`);
        detailResult.textContent = JSON.stringify(result.data, null, 2);
        backendInstance.textContent = result.backendInstance;
        nginxUpstream.textContent = result.nginxUpstream;
        detailStatus.textContent = "请求成功";
    } catch (error) {
        detailStatus.textContent = "请求失败";
        detailResult.textContent = error.message;
        backendInstance.textContent = "-";
        nginxUpstream.textContent = "-";
    }
}

refreshBtn.addEventListener("click", loadList);
lbModeSelect.addEventListener("change", loadList);

loadList();
