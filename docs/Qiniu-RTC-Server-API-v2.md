# 1. 概述

Qiniu RTC Server API 提供为 Qiniu 连麦 SDK 提供了权限验证和房间管理功能，API 均采用 REST 接口。

# 2. HTTP请求鉴权

Qiniu RTC Server API 通过 Qiniu Authorization 方式进行鉴权，每个房间管理HTTP 请求头部需增加一个 Authorization 字段：

```
Authorization: "<QiniuToken>"
```

**QiniuToken**: 管理凭证，用于鉴权。

使用七牛颁发的 `AccessKey` 和 `SecretKey` ，对本次 http 请求的信息进行签名，生成管理凭证。签名的原始数据包括 http 请求的 `Method`, `Path`, `RawQuery`, `Content-Type `及 `Body` 等信息，这些信息的获取方法取决于具体所用的编程语言，建议参照七牛提供的SDK代码。

计算过程及伪代码如下：

```
// 1.构造待签名的 Data

// 添加 Method 和 Path
data = "<Method> <Path>"

// 添加 Query，如果Query不存在或者为空，则跳过此步
if "<RawQuery>" != "" {
        data += "?<RawQuery>"
}

// 添加 Host
data += "\nHost: <Host>"

// 添加 Content-Type，如果Content-Type不存在或者为空，则跳过此步
if "<Content-Type>" != "" {
        data += "\nContent-Type: <Content-Type>"
}

// 添加回车
data += "\n\n"

// 添加 Body， 如果Content-Length, Content-Type和Body任意一个不存在或者为空，则跳过此步；如果Content-Type为application/octet-stream，也跳过此步
bodyOK := "<Content-Length>" != "" && "<Body>" != ""
contentTypeOK := "<Content-Type>" != "" && "<Content-Type>" != "application/octet-stream"
if bodyOK && contentTypeOK {
        data += "<Body>"
}

// 2. 计算 HMAC-SHA1 签名，并对签名结果做 URL 安全的 Base64 编码
sign = hmac_sha1(data, "Your_Secret_Key")
encodedSign = urlsafe_base64_encode(sign)  

// 3. 将 Qiniu 标识与 AccessKey、encodedSign 拼接得到管理凭证
<QiniuToken> = "Qiniu " + "Your_Access_Key" + ":" + encodedSign
```

# 3. 创建房间

## 3.1 请求包

```
POST /v2/rooms
Host: rtc.qiniuapi.com 
Authorization: <QiniuToken> 
Content-Type: application/json
{
    "owner_id": "<OwnerUserId>",
    "room_name": "<RoomName>",
    "user_max": "<UserMax>"
}
```

**OwnerUserId**: 要创建房间的所有者，最大长度50，需满足规格`^[a-zA-Z0-9_-]{3,50}$`

**RoomName**: 要创建的房间名称，可选，最大长度64位，需满足规格`^[a-zA-Z0-9_-]{3,64}$`。如果没有指定，系统返回一个uuid作为房间名称。

**UserMax**: int类型，该房间支持的最大会议人数，可选。如果没有指定，则默认最多3人。

## 3.2 返回包

```
200 OK
{   
    "room_name": "<RoomName>"
}
400
{
    "error": "invalid args"
}
611 
{
    "error": "room already exist"
}
```

**RoomName**: 已创建的房间名称。

# 4. 查看房间

## 4.1 请求包

```
GET /v2/rooms/<RoomName> 
Host: rtc.qiniuapi.com 
Authorization: <QiniuToken> 
```

**RoomName**: 房间名称。

## 4.2 返回包

```
200 OK 
{
    "room_name": "<RoomName>",
    "owner_id": "<OwnerUserID>",
    "room_status": <RoomStatus>,
    "user_max": "<UserMax>"
}
612 
{
    "error": "room not found"
}
```

**RoomName**: 房间名称。

**OwnerUserId**: 房间的所有者。

**RoomStatus**: enum类型，房间状态，0 刚创建，1 房间正在进行会议，2 房间会议已经结束。

**UserMax**: int类型，该房间支持的最大会议人数。

# 5. 删除房间

## 5.1 请求包

```

DELETE /v2/rooms/<RoomName> 
Host: rtc.qiniuapi.com 
Authorization: <QiniuToken>
```

**RoomName**: 房间名称

## 5.2 返回包

```

200 OK
612
{
    "error": "room not found"
}
613
{
    "error": "room in use"
}

```

注意，正在进行视频会议的房间（RoomStatus为1）无法删除。

# 6. RoomToken 的计算

连麦用户终端通过房间管理鉴权获取七牛连麦服务，该鉴权包含了房间名称、用户ID、用户权限、有效时间等信息，需要通过客户的业务服务器使用七牛颁发的AccessKey和SecretKey进行签算并分发给手机APP。手机端SDK以拟定的用户ID身份连接服务器，加入该房间进行视频会议。若用户ID或房间与token内的签算信息不符，则无法通过鉴权加入房间。

计算方法：

```

// 1. 定义房间管理凭证，并对凭证字符做URL安全的Base64编码
roomAccess = {
	"version": "<Version>"
    "room_name": "<RoomName>",
    "user_id": "<UserID>",
    "perm": "<Permission>",
    "expire_at": <ExpireAt>
}
roomAccessString = json_to_string(roomAccess) 
encodedRoomAccess = urlsafe_base64_encode(roomAccessString)

// 2. 计算HMAC-SHA1签名，并对签名结果做URL安全的Base64编码
sign = hmac_sha1(encodedRoomAccess, <SecretKey>)
encodedSign = urlsafe_base64_encode(sign)

// 3. 将AccessKey与以上两者拼接得到房间鉴权
roomToken = "<AccessKey>" + ":" + encodedSign + ":" + encodedRoomAccess
```

**RoomName**: 房间名称，需满足规格`^[a-zA-Z0-9_-]{3,64}$`

**UserID**: 请求加入房间的用户ID，需满足规格`^[a-zA-Z0-9_-]{3,50}$`

**Permission**: 该用户的房间管理权限，"admin"或"user"，房间主播为"admin"，拥有将其他用户移除出房间等特权

**ExpireAt**: int64类型，鉴权的有效时间，传入以秒为单位的64位Unix绝对时间，token将在该时间后失效

**Version**: 版本号，字符串；当前版本 "2.0"



