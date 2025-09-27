# SparkCraft 用户管理 API 接口文档

## 基础信息
- **基础URL**: `http://localhost:8080`
- **API前缀**: `/user`
- **数据格式**: JSON
- **字符编码**: UTF-8

## 通用响应格式

所有接口都使用统一的响应格式：

```json
{
  "code": 0,
  "data": {},
  "message": "ok"
}
```

- `code`: 状态码，0表示成功，非0表示失败
- `data`: 响应数据
- `message`: 响应消息

## 接口列表

### 1. 用户注册

**接口名称**: 用户注册  
**URL**: `POST /user/register`  
**描述**: 新用户注册账号

#### 请求参数

| 参数名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| userAccount | String | 是 | 用户账号，长度不少于4位 |
| userPassword | String | 是 | 用户密码，长度不少于8位 |
| checkPassword | String | 是 | 确认密码，需与密码一致 |

#### 请求示例

```json
{
  "userAccount": "testuser",
  "userPassword": "12345678",
  "checkPassword": "12345678"
}
```

#### 响应参数

| 参数名 | 类型 | 描述 |
|--------|------|------|
| code | Integer | 状态码 |
| data | Long | 新用户ID |
| message | String | 响应消息 |

#### 响应示例

```json
{
  "code": 0,
  "data": 1234567890,
  "message": "ok"
}
```

---

### 2. 用户登录

**接口名称**: 用户登录  
**URL**: `POST /user/login`  
**描述**: 用户账号密码登录

#### 请求参数

| 参数名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| userAccount | String | 是 | 用户账号 |
| userPassword | String | 是 | 用户密码 |

#### 请求示例

```
POST /user/login
Content-Type: application/x-www-form-urlencoded

userAccount=testuser&userPassword=12345678
```

#### 响应参数

| 参数名 | 类型 | 描述 |
|--------|------|------|
| code | Integer | 状态码 |
| data | LoginUserVO | 登录用户信息 |
| message | String | 响应消息 |

**LoginUserVO 结构**:

| 参数名 | 类型 | 描述 |
|--------|------|------|
| id | Long | 用户ID |
| userAccount | String | 用户账号 |
| userName | String | 用户昵称 |
| userAvatar | String | 用户头像 |
| userProfile | String | 用户简介 |
| userRole | String | 用户角色 |
| createTime | LocalDateTime | 创建时间 |
| updateTime | LocalDateTime | 更新时间 |

#### 响应示例

```json
{
  "code": 0,
  "data": {
    "id": 1234567890,
    "userAccount": "testuser",
    "userName": "测试用户",
    "userAvatar": "https://example.com/avatar.jpg",
    "userProfile": "这是一个测试用户",
    "userRole": "user",
    "createTime": "2024-01-01T10:00:00",
    "updateTime": "2024-01-01T10:00:00"
  },
  "message": "ok"
}
```

---

### 3. 获取当前登录用户

**接口名称**: 获取当前登录用户  
**URL**: `GET /user/get/login`  
**描述**: 获取当前登录用户信息

#### 请求参数

无需参数，通过Session获取当前登录用户

#### 请求示例

```
GET /user/get/login
```

#### 响应参数

| 参数名 | 类型 | 描述 |
|--------|------|------|
| code | Integer | 状态码 |
| data | LoginUserVO | 当前登录用户信息 |
| message | String | 响应消息 |

#### 响应示例

```json
{
  "code": 0,
  "data": {
    "id": 1234567890,
    "userAccount": "testuser",
    "userName": "测试用户",
    "userAvatar": "https://example.com/avatar.jpg",
    "userProfile": "这是一个测试用户",
    "userRole": "user",
    "createTime": "2024-01-01T10:00:00",
    "updateTime": "2024-01-01T10:00:00"
  },
  "message": "ok"
}
```

---

### 4. 用户注销

**接口名称**: 用户注销  
**URL**: `POST /user/logout`  
**描述**: 用户退出登录

#### 请求参数

无需参数

#### 请求示例

```
POST /user/logout
```

#### 响应参数

| 参数名 | 类型 | 描述 |
|--------|------|------|
| code | Integer | 状态码 |
| data | Boolean | 注销结果 |
| message | String | 响应消息 |

#### 响应示例

```json
{
  "code": 0,
  "data": true,
  "message": "ok"
}
```

---

### 5. 创建用户（管理员）

**接口名称**: 创建用户  
**URL**: `POST /user/add`  
**描述**: 管理员创建新用户  
**权限**: 需要管理员权限

#### 请求参数

| 参数名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| userName | String | 否 | 用户昵称 |
| userAccount | String | 是 | 用户账号 |
| userAvatar | String | 否 | 用户头像URL |
| userProfile | String | 否 | 用户简介 |
| userRole | String | 否 | 用户角色（user/admin） |

#### 请求示例

```json
{
  "userName": "新用户",
  "userAccount": "newuser",
  "userAvatar": "https://example.com/avatar.jpg",
  "userProfile": "这是一个新用户",
  "userRole": "user"
}
```

#### 响应参数

| 参数名 | 类型 | 描述 |
|--------|------|------|
| code | Integer | 状态码 |
| data | Long | 新用户ID |
| message | String | 响应消息 |

#### 响应示例

```json
{
  "code": 0,
  "data": 1234567891,
  "message": "ok"
}
```

---

### 6. 根据ID获取用户（管理员）

**接口名称**: 根据ID获取用户  
**URL**: `GET /user/get?id={id}`  
**描述**: 管理员根据ID获取用户详细信息  
**权限**: 需要管理员权限

#### 请求参数

| 参数名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| id | Long | 是 | 用户ID |

#### 请求示例

```
GET /user/get?id=1234567890
```

#### 响应参数

| 参数名 | 类型 | 描述 |
|--------|------|------|
| code | Integer | 状态码 |
| data | User | 用户完整信息 |
| message | String | 响应消息 |

#### 响应示例

```json
{
  "code": 0,
  "data": {
    "id": 1234567890,
    "userAccount": "testuser",
    "userName": "测试用户",
    "userAvatar": "https://example.com/avatar.jpg",
    "userProfile": "这是一个测试用户",
    "userRole": "user",
    "createTime": "2024-01-01T10:00:00",
    "updateTime": "2024-01-01T10:00:00",
    "editTime": "2024-01-01T10:00:00",
    "isDelete": 0
  },
  "message": "ok"
}
```

---

### 7. 根据ID获取用户VO

**接口名称**: 根据ID获取用户VO  
**URL**: `GET /user/get/vo?id={id}`  
**描述**: 获取用户脱敏信息

#### 请求参数

| 参数名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| id | Long | 是 | 用户ID |

#### 请求示例

```
GET /user/get/vo?id=1234567890
```

#### 响应参数

| 参数名 | 类型 | 描述 |
|--------|------|------|
| code | Integer | 状态码 |
| data | UserVO | 用户脱敏信息 |
| message | String | 响应消息 |

**UserVO 结构**:

| 参数名 | 类型 | 描述 |
|--------|------|------|
| id | Long | 用户ID |
| userAccount | String | 用户账号 |
| userName | String | 用户昵称 |
| userAvatar | String | 用户头像 |
| userProfile | String | 用户简介 |
| userRole | String | 用户角色 |
| createTime | LocalDateTime | 创建时间 |

#### 响应示例

```json
{
  "code": 0,
  "data": {
    "id": 1234567890,
    "userAccount": "testuser",
    "userName": "测试用户",
    "userAvatar": "https://example.com/avatar.jpg",
    "userProfile": "这是一个测试用户",
    "userRole": "user",
    "createTime": "2024-01-01T10:00:00"
  },
  "message": "ok"
}
```

---

### 8. 删除用户（管理员）

**接口名称**: 删除用户  
**URL**: `POST /user/delete`  
**描述**: 管理员删除用户  
**权限**: 需要管理员权限

#### 请求参数

| 参数名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| id | Long | 是 | 要删除的用户ID |

#### 请求示例

```json
{
  "id": 1234567890
}
```

#### 响应参数

| 参数名 | 类型 | 描述 |
|--------|------|------|
| code | Integer | 状态码 |
| data | Boolean | 删除结果 |
| message | String | 响应消息 |

#### 响应示例

```json
{
  "code": 0,
  "data": true,
  "message": "ok"
}
```

---

### 9. 更新用户（管理员）

**接口名称**: 更新用户  
**URL**: `POST /user/update`  
**描述**: 管理员更新用户信息  
**权限**: 需要管理员权限

#### 请求参数

| 参数名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| id | Long | 是 | 用户ID |
| userName | String | 否 | 用户昵称 |
| userAvatar | String | 否 | 用户头像URL |
| userProfile | String | 否 | 用户简介 |
| userRole | String | 否 | 用户角色（user/admin） |

#### 请求示例

```json
{
  "id": 1234567890,
  "userName": "更新后的用户名",
  "userAvatar": "https://example.com/new-avatar.jpg",
  "userProfile": "更新后的用户简介",
  "userRole": "user"
}
```

#### 响应参数

| 参数名 | 类型 | 描述 |
|--------|------|------|
| code | Integer | 状态码 |
| data | Boolean | 更新结果 |
| message | String | 响应消息 |

#### 响应示例

```json
{
  "code": 0,
  "data": true,
  "message": "ok"
}
```

---

### 10. 分页获取用户列表（管理员）

**接口名称**: 分页获取用户列表  
**URL**: `POST /user/list/page/vo`  
**描述**: 管理员分页查询用户列表  
**权限**: 需要管理员权限

#### 请求参数

| 参数名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| pageNum | Integer | 否 | 页码，默认1 |
| pageSize | Integer | 否 | 每页大小，默认10 |
| sortField | String | 否 | 排序字段 |
| sortOrder | String | 否 | 排序方式（ascend/descend），默认descend |
| id | Long | 否 | 用户ID（筛选条件） |
| userName | String | 否 | 用户昵称（模糊查询） |
| userAccount | String | 否 | 用户账号（模糊查询） |
| userProfile | String | 否 | 用户简介（模糊查询） |
| userRole | String | 否 | 用户角色（筛选条件） |

#### 请求示例

```json
{
  "pageNum": 1,
  "pageSize": 10,
  "sortField": "createTime",
  "sortOrder": "descend",
  "userName": "测试",
  "userRole": "user"
}
```

#### 响应参数

| 参数名 | 类型 | 描述 |
|--------|------|------|
| code | Integer | 状态码 |
| data | Page&lt;UserVO&gt; | 分页用户信息 |
| message | String | 响应消息 |

**Page&lt;UserVO&gt; 结构**:

| 参数名 | 类型 | 描述 |
|--------|------|------|
| pageNumber | Long | 当前页码 |
| pageSize | Long | 每页大小 |
| totalRow | Long | 总记录数 |
| records | List&lt;UserVO&gt; | 用户列表 |

#### 响应示例

```json
{
  "code": 0,
  "data": {
    "pageNumber": 1,
    "pageSize": 10,
    "totalRow": 100,
    "records": [
      {
        "id": 1234567890,
        "userAccount": "testuser1",
        "userName": "测试用户1",
        "userAvatar": "https://example.com/avatar1.jpg",
        "userProfile": "这是测试用户1",
        "userRole": "user",
        "createTime": "2024-01-01T10:00:00"
      },
      {
        "id": 1234567891,
        "userAccount": "testuser2",
        "userName": "测试用户2",
        "userAvatar": "https://example.com/avatar2.jpg",
        "userProfile": "这是测试用户2",
        "userRole": "user",
        "createTime": "2024-01-01T11:00:00"
      }
    ]
  },
  "message": "ok"
}
```

---

## 错误码说明

| 错误码 | 描述 |
|--------|------|
| 0 | 成功 |
| 40000 | 请求参数错误 |
| 40001 | 请求数据为空 |
| 40101 | 未登录 |
| 40301 | 无权限 |
| 40400 | 请求数据不存在 |
| 50000 | 系统内部异常 |
| 50001 | 操作失败 |

## 注意事项

1. 所有需要登录的接口都需要在请求头中携带有效的Session信息
2. 标记为"管理员"权限的接口只有管理员角色的用户才能访问
3. 密码在传输和存储时都会进行加密处理
4. 用户角色包括：`user`（普通用户）、`admin`（管理员）
5. 所有时间字段格式为：`yyyy-MM-ddTHH:mm:ss`
6. 分页查询支持多种排序和筛选条件
7. 删除操作为逻辑删除，不会真正删除数据库记录