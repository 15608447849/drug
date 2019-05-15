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
public interface InterfacesPrx extends Ice.ObjectPrx
{
    /**
     * 前后台交互
     **/
    public String accessService(IRequest request);

    /**
     * 前后台交互
     * @param __ctx The Context map to send with the invocation.
     **/
    public String accessService(IRequest request, java.util.Map<String, String> __ctx);

    /**
     * 前后台交互
     * @return The asynchronous result object.
     **/
    public Ice.AsyncResult begin_accessService(IRequest request);

    /**
     * 前后台交互
     * @param __ctx The Context map to send with the invocation.
     * @return The asynchronous result object.
     **/
    public Ice.AsyncResult begin_accessService(IRequest request, java.util.Map<String, String> __ctx);

    /**
     * 前后台交互
     * @param __cb The asynchronous callback object.
     * @return The asynchronous result object.
     **/
    public Ice.AsyncResult begin_accessService(IRequest request, Ice.Callback __cb);

    /**
     * 前后台交互
     * @param __ctx The Context map to send with the invocation.
     * @param __cb The asynchronous callback object.
     * @return The asynchronous result object.
     **/
    public Ice.AsyncResult begin_accessService(IRequest request, java.util.Map<String, String> __ctx, Ice.Callback __cb);

    /**
     * 前后台交互
     * @param __cb The asynchronous callback object.
     * @return The asynchronous result object.
     **/
    public Ice.AsyncResult begin_accessService(IRequest request, Callback_Interfaces_accessService __cb);

    /**
     * 前后台交互
     * @param __ctx The Context map to send with the invocation.
     * @param __cb The asynchronous callback object.
     * @return The asynchronous result object.
     **/
    public Ice.AsyncResult begin_accessService(IRequest request, java.util.Map<String, String> __ctx, Callback_Interfaces_accessService __cb);

    /**
     * 前后台交互
     * @param __responseCb The lambda response callback.
     * @param __exceptionCb The lambda exception callback.
     * @return The asynchronous result object.
     **/
    public Ice.AsyncResult begin_accessService(IRequest request, 
                                               IceInternal.Functional_GenericCallback1<String> __responseCb, 
                                               IceInternal.Functional_GenericCallback1<Ice.Exception> __exceptionCb);

    /**
     * 前后台交互
     * @param __responseCb The lambda response callback.
     * @param __exceptionCb The lambda exception callback.
     * @param __sentCb The lambda sent callback.
     * @return The asynchronous result object.
     **/
    public Ice.AsyncResult begin_accessService(IRequest request, 
                                               IceInternal.Functional_GenericCallback1<String> __responseCb, 
                                               IceInternal.Functional_GenericCallback1<Ice.Exception> __exceptionCb, 
                                               IceInternal.Functional_BoolCallback __sentCb);

    /**
     * 前后台交互
     * @param __ctx The Context map to send with the invocation.
     * @param __responseCb The lambda response callback.
     * @param __exceptionCb The lambda exception callback.
     * @return The asynchronous result object.
     **/
    public Ice.AsyncResult begin_accessService(IRequest request, 
                                               java.util.Map<String, String> __ctx, 
                                               IceInternal.Functional_GenericCallback1<String> __responseCb, 
                                               IceInternal.Functional_GenericCallback1<Ice.Exception> __exceptionCb);

    /**
     * 前后台交互
     * @param __ctx The Context map to send with the invocation.
     * @param __responseCb The lambda response callback.
     * @param __exceptionCb The lambda exception callback.
     * @param __sentCb The lambda sent callback.
     * @return The asynchronous result object.
     **/
    public Ice.AsyncResult begin_accessService(IRequest request, 
                                               java.util.Map<String, String> __ctx, 
                                               IceInternal.Functional_GenericCallback1<String> __responseCb, 
                                               IceInternal.Functional_GenericCallback1<Ice.Exception> __exceptionCb, 
                                               IceInternal.Functional_BoolCallback __sentCb);

    /**
     * 前后台交互
     * @param __result The asynchronous result object.
     **/
    public String end_accessService(Ice.AsyncResult __result);

    /**
     * 消息推送-服务端 / 客户端上线
     **/
    public void online(Ice.Identity identity);

    /**
     * 消息推送-服务端 / 客户端上线
     * @param __ctx The Context map to send with the invocation.
     **/
    public void online(Ice.Identity identity, java.util.Map<String, String> __ctx);

    /**
     * 消息推送-服务端 / 客户端上线
     * @return The asynchronous result object.
     **/
    public Ice.AsyncResult begin_online(Ice.Identity identity);

    /**
     * 消息推送-服务端 / 客户端上线
     * @param __ctx The Context map to send with the invocation.
     * @return The asynchronous result object.
     **/
    public Ice.AsyncResult begin_online(Ice.Identity identity, java.util.Map<String, String> __ctx);

    /**
     * 消息推送-服务端 / 客户端上线
     * @param __cb The asynchronous callback object.
     * @return The asynchronous result object.
     **/
    public Ice.AsyncResult begin_online(Ice.Identity identity, Ice.Callback __cb);

    /**
     * 消息推送-服务端 / 客户端上线
     * @param __ctx The Context map to send with the invocation.
     * @param __cb The asynchronous callback object.
     * @return The asynchronous result object.
     **/
    public Ice.AsyncResult begin_online(Ice.Identity identity, java.util.Map<String, String> __ctx, Ice.Callback __cb);

    /**
     * 消息推送-服务端 / 客户端上线
     * @param __cb The asynchronous callback object.
     * @return The asynchronous result object.
     **/
    public Ice.AsyncResult begin_online(Ice.Identity identity, Callback_Interfaces_online __cb);

    /**
     * 消息推送-服务端 / 客户端上线
     * @param __ctx The Context map to send with the invocation.
     * @param __cb The asynchronous callback object.
     * @return The asynchronous result object.
     **/
    public Ice.AsyncResult begin_online(Ice.Identity identity, java.util.Map<String, String> __ctx, Callback_Interfaces_online __cb);

    /**
     * 消息推送-服务端 / 客户端上线
     * @param __responseCb The lambda response callback.
     * @param __exceptionCb The lambda exception callback.
     * @return The asynchronous result object.
     **/
    public Ice.AsyncResult begin_online(Ice.Identity identity, 
                                        IceInternal.Functional_VoidCallback __responseCb, 
                                        IceInternal.Functional_GenericCallback1<Ice.Exception> __exceptionCb);

    /**
     * 消息推送-服务端 / 客户端上线
     * @param __responseCb The lambda response callback.
     * @param __exceptionCb The lambda exception callback.
     * @param __sentCb The lambda sent callback.
     * @return The asynchronous result object.
     **/
    public Ice.AsyncResult begin_online(Ice.Identity identity, 
                                        IceInternal.Functional_VoidCallback __responseCb, 
                                        IceInternal.Functional_GenericCallback1<Ice.Exception> __exceptionCb, 
                                        IceInternal.Functional_BoolCallback __sentCb);

    /**
     * 消息推送-服务端 / 客户端上线
     * @param __ctx The Context map to send with the invocation.
     * @param __responseCb The lambda response callback.
     * @param __exceptionCb The lambda exception callback.
     * @return The asynchronous result object.
     **/
    public Ice.AsyncResult begin_online(Ice.Identity identity, 
                                        java.util.Map<String, String> __ctx, 
                                        IceInternal.Functional_VoidCallback __responseCb, 
                                        IceInternal.Functional_GenericCallback1<Ice.Exception> __exceptionCb);

    /**
     * 消息推送-服务端 / 客户端上线
     * @param __ctx The Context map to send with the invocation.
     * @param __responseCb The lambda response callback.
     * @param __exceptionCb The lambda exception callback.
     * @param __sentCb The lambda sent callback.
     * @return The asynchronous result object.
     **/
    public Ice.AsyncResult begin_online(Ice.Identity identity, 
                                        java.util.Map<String, String> __ctx, 
                                        IceInternal.Functional_VoidCallback __responseCb, 
                                        IceInternal.Functional_GenericCallback1<Ice.Exception> __exceptionCb, 
                                        IceInternal.Functional_BoolCallback __sentCb);

    /**
     * 消息推送-服务端 / 客户端上线
     * @param __result The asynchronous result object.
     **/
    public void end_online(Ice.AsyncResult __result);

    /**
     * 消息推送-服务端 / 后端服务调用 - 向指定客户端发送消息
     **/
    public void sendMessageToClient(String identityName, String message);

    /**
     * 消息推送-服务端 / 后端服务调用 - 向指定客户端发送消息
     * @param __ctx The Context map to send with the invocation.
     **/
    public void sendMessageToClient(String identityName, String message, java.util.Map<String, String> __ctx);

    /**
     * 消息推送-服务端 / 后端服务调用 - 向指定客户端发送消息
     * @return The asynchronous result object.
     **/
    public Ice.AsyncResult begin_sendMessageToClient(String identityName, String message);

    /**
     * 消息推送-服务端 / 后端服务调用 - 向指定客户端发送消息
     * @param __ctx The Context map to send with the invocation.
     * @return The asynchronous result object.
     **/
    public Ice.AsyncResult begin_sendMessageToClient(String identityName, String message, java.util.Map<String, String> __ctx);

    /**
     * 消息推送-服务端 / 后端服务调用 - 向指定客户端发送消息
     * @param __cb The asynchronous callback object.
     * @return The asynchronous result object.
     **/
    public Ice.AsyncResult begin_sendMessageToClient(String identityName, String message, Ice.Callback __cb);

    /**
     * 消息推送-服务端 / 后端服务调用 - 向指定客户端发送消息
     * @param __ctx The Context map to send with the invocation.
     * @param __cb The asynchronous callback object.
     * @return The asynchronous result object.
     **/
    public Ice.AsyncResult begin_sendMessageToClient(String identityName, String message, java.util.Map<String, String> __ctx, Ice.Callback __cb);

    /**
     * 消息推送-服务端 / 后端服务调用 - 向指定客户端发送消息
     * @param __cb The asynchronous callback object.
     * @return The asynchronous result object.
     **/
    public Ice.AsyncResult begin_sendMessageToClient(String identityName, String message, Callback_Interfaces_sendMessageToClient __cb);

    /**
     * 消息推送-服务端 / 后端服务调用 - 向指定客户端发送消息
     * @param __ctx The Context map to send with the invocation.
     * @param __cb The asynchronous callback object.
     * @return The asynchronous result object.
     **/
    public Ice.AsyncResult begin_sendMessageToClient(String identityName, String message, java.util.Map<String, String> __ctx, Callback_Interfaces_sendMessageToClient __cb);

    /**
     * 消息推送-服务端 / 后端服务调用 - 向指定客户端发送消息
     * @param __responseCb The lambda response callback.
     * @param __exceptionCb The lambda exception callback.
     * @return The asynchronous result object.
     **/
    public Ice.AsyncResult begin_sendMessageToClient(String identityName, 
                                                     String message, 
                                                     IceInternal.Functional_VoidCallback __responseCb, 
                                                     IceInternal.Functional_GenericCallback1<Ice.Exception> __exceptionCb);

    /**
     * 消息推送-服务端 / 后端服务调用 - 向指定客户端发送消息
     * @param __responseCb The lambda response callback.
     * @param __exceptionCb The lambda exception callback.
     * @param __sentCb The lambda sent callback.
     * @return The asynchronous result object.
     **/
    public Ice.AsyncResult begin_sendMessageToClient(String identityName, 
                                                     String message, 
                                                     IceInternal.Functional_VoidCallback __responseCb, 
                                                     IceInternal.Functional_GenericCallback1<Ice.Exception> __exceptionCb, 
                                                     IceInternal.Functional_BoolCallback __sentCb);

    /**
     * 消息推送-服务端 / 后端服务调用 - 向指定客户端发送消息
     * @param __ctx The Context map to send with the invocation.
     * @param __responseCb The lambda response callback.
     * @param __exceptionCb The lambda exception callback.
     * @return The asynchronous result object.
     **/
    public Ice.AsyncResult begin_sendMessageToClient(String identityName, 
                                                     String message, 
                                                     java.util.Map<String, String> __ctx, 
                                                     IceInternal.Functional_VoidCallback __responseCb, 
                                                     IceInternal.Functional_GenericCallback1<Ice.Exception> __exceptionCb);

    /**
     * 消息推送-服务端 / 后端服务调用 - 向指定客户端发送消息
     * @param __ctx The Context map to send with the invocation.
     * @param __responseCb The lambda response callback.
     * @param __exceptionCb The lambda exception callback.
     * @param __sentCb The lambda sent callback.
     * @return The asynchronous result object.
     **/
    public Ice.AsyncResult begin_sendMessageToClient(String identityName, 
                                                     String message, 
                                                     java.util.Map<String, String> __ctx, 
                                                     IceInternal.Functional_VoidCallback __responseCb, 
                                                     IceInternal.Functional_GenericCallback1<Ice.Exception> __exceptionCb, 
                                                     IceInternal.Functional_BoolCallback __sentCb);

    /**
     * 消息推送-服务端 / 后端服务调用 - 向指定客户端发送消息
     * @param __result The asynchronous result object.
     **/
    public void end_sendMessageToClient(Ice.AsyncResult __result);
}
