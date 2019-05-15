// **********************************************************************
//
// Copyright (c) 2003-2016 ZeroC, Inc. All rights reserved.
//
// This copy of Ice is licensed to you under the terms described in the
// ICE_LICENSE file included in this distribution.
//
// **********************************************************************
//
// Ice version 3.6.3
//
// <auto-generated>
//
// Generated from file `iceInterfaces.ice'
//
// Warning: do not edit this file.
//
// </auto-generated>
//

package com.onek.server.inf;

/**
 * 服务接口 interface
 **/
public interface _InterfacesOperationsNC
{
    /**
     * 前后台交互
     **/
    String accessService(IRequest request);

    /**
     * 消息推送-服务端 / 客户端上线
     **/
    void online(Ice.Identity identity);

    /**
     * 消息推送-服务端 / 后端服务调用 - 向指定客户端发送消息
     **/
    void sendMessageToClient(String identityName, String message);
}
