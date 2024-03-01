/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2024 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *    SECU-3  - An open source, free engine control unit
 *    Copyright (C) 2007-2024 Alexey A. Shabelnikov. Ukraine, Kyiv
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *    contacts:
 *                    http://secu-3.org
 *                    email: vetalkosharskiy@gmail.com
 */

package org.secu3.android.network

import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody
import org.secu3.android.utils.NetworkHelper

class ConnectivityInterceptor (private val networkHelper: NetworkHelper) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        if (networkHelper.isNetworkAvailable().not()) {
            return Response.Builder()
                .code(600) // Internal server error
                .message("No internet connection")
                .body(ResponseBody.create(null, "Empty body from interceptor"))
                .protocol(Protocol.HTTP_1_1)
                .request(chain.request())
                .build()
        }

        val builder = chain.request().newBuilder()
        return chain.proceed(builder.build())
    }
}