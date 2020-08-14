import socket
import json

serv = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

serv.bind(('0.0.0.0', 18005))
serv.listen(1)

dataJson = [
    "hello",
    "how are you"
]

jsonData = json.dumps(dataJson)

while True:
    conn, addr = serv.accept()

    while True:

        data = conn.recv(4096)
        if not data: break

        conn.send(('{type:"predict",data:' + jsonData + '}\n').encode())

    conn.close()
    print('client disconnected')
