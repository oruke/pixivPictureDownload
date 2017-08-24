/**
 * Created by oruke on 2017.08.01.
 */

let socket = new WebSocket('ws://localhost:9200/websocket/download');
socket.onmessage = function (res) {
    if (res.isTrusted) {
        let data = JSON.parse(res.data);
        if (data["type"] === "info") {
            $("#message").append("<div class=\"alert "+data["class"]+"\" role=\"alert\">" + data["message"] + "</div>");
            $("#message").animate({scrollTop: 100000}, 500);
            return;
        }
        $.each(data, function (key, value) {
            if (key !== "type") {
                $("#message").append("<div class=\"alert alert-info\" role=\"alert\">正在下载：" + key + "</div>");
                $("#message").animate({scrollTop: 100000}, 500);
                let img = key.split("\.");
                let blob = new Blob([bytesToUnit8(value)], {type: "image/" + img[img.length - 1] + ";charset=utf-8"});
                saveAs(blob, key);
                $("#message").append("<div class=\"alert alert-success\" role=\"alert\">下载完成：" + key + "</div>");
                $("#message").animate({scrollTop: 100000}, 500);
            }
        });
    }
};

$("#browser").on("click", function () {
    let params = $("#config-form").serializeJSON();
    params["downloadMeans"] = "BROWSER";
    socket.send(JSON.stringify(params));
});

$("#local").on("click", function () {
    let params = $("#config-form").serializeJSON();
    params["downloadMeans"] = "LOCAL";
    socket.send(JSON.stringify(params));
});

function bytesToUnit8(byteArray) {
    let bytes = new Uint8Array(byteArray.length);
    for (let i = 0; i < byteArray.length; i++) {
        bytes[i] = byteArray[i];
    }
    return bytes;
}

function strToUnit8(str) {
    let bytes = new Uint8Array(str.length);
    for (let i = 0; i < str.length; i++) {
        bytes[i] = str.charCodeAt(i);
    }
    return bytes;
}

