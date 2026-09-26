#!/usr/bin/env python3
"""Black-box checks against an actual HTTP server. Standard library only.
Creates ONLY disposable, uniquely named data and deletes its own test account.
Usage: python3 scripts/smoke.py http://localhost:8080
"""
import json
import sys
import time
import uuid
import urllib.request
import urllib.error

base = (sys.argv[1] if len(sys.argv) > 1 else 'http://localhost:8080').rstrip('/')
token = None
checks = 0

def request(method, path, expected, body=None, auth=True, extra=None):
    global checks
    headers = dict(extra or {})
    if auth and token:
        headers['Authorization'] = 'Bearer ' + token
    payload = json.dumps(body).encode() if body is not None else None
    if payload is not None:
        headers['Content-Type'] = 'application/json'
    req = urllib.request.Request(base + path, data=payload, headers=headers, method=method)
    try:
        response = urllib.request.urlopen(req, timeout=100)
    except urllib.error.HTTPError as error:
        response = error
    with response:
        raw = response.read()
        assert response.status == expected, f'{method} {path}: expected {expected}, got {response.status}'
        checks += 1
        print(f'PASS {method} {path} → {response.status}')
        return (json.loads(raw) if raw else None), response.headers

for attempt in range(60):
    try:
        health, _ = request('GET', '/api/health', 200, auth=False)
        assert health['status'] == 'UP'
        break
    except (OSError, AssertionError):
        if attempt == 59:
            raise
        time.sleep(2)

email = 'smoke-' + str(uuid.uuid4()) + '@example.test'
password = 'Temporary-training-' + uuid.uuid4().hex[:8]
try:
    body, _ = request('HEAD', '/api/catalog', 200, auth=False)
    assert body is None, 'HEAD must not return an entity over the wire'
    request('GET', '/api/me', 401, auth=False)
    session, _ = request('POST', '/api/auth/register', 201, {'email':email,'password':password,'name':'Smoke QA'}, auth=False)
    token = session['token']
    user, _ = request('GET', '/api/me', 200)
    assert user['email'] == email and 'password' not in user
    product, headers = request('POST', '/api/products', 201, {'name':'Smoke product','price':350,'stock':5})
    assert headers['Location'].endswith(product['id'])
    path = '/api/products/' + product['id']
    request('POST', '/api/products', 400, {'name':'','price':-1,'stock':2})
    product, _ = request('PATCH', path, 200, {'stock':4})
    assert product['price'] == 350
    payload = {'productId':product['id'],'quantity':2}
    order, _ = request('POST', '/api/orders', 201, payload, extra={'Idempotency-Key':'smoke-key'})
    replay, headers = request('POST', '/api/orders', 200, payload, extra={'Idempotency-Key':'smoke-key'})
    assert order['id'] == replay['id'] and headers['Idempotency-Replayed'] == 'true'
    assert order['total'] == 700
    product, _ = request('GET', path, 200)
    assert product['stock'] == 2
    request('POST', '/api/orders', 409, {'productId':product['id'],'quantity':20})
    request('POST', '/api/orders/' + order['id'] + '/pay', 402, {'outcome':'DECLINED'})
    request('POST', '/api/orders/' + order['id'] + '/cancel', 200)
    request('POST', '/api/orders/' + order['id'] + '/cancel', 200)
    product, _ = request('GET', path, 200)
    assert product['stock'] == 4
    request('DELETE', path, 204)
    request('GET', path, 404)
    request('GET', '/api/admin/stats', 403)
    request('POST', '/api/sandbox/reset', 200)
    print(f'All {checks} HTTP checks passed.')
finally:
    if token:
        request('DELETE', '/api/me', 204)
