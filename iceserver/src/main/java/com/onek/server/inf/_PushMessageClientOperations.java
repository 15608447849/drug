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
 * 消息推送-客户端 需要具体客户端实现
 **/
public interface _PushMessageClientOperations
{
    /**
     * 客户端接受服务端 消息
     * @param __current The Current object for the invocation.
     **/
    void receive(String message, Ice.Current __current);
}
