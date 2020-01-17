from rest_framework.permissions import AllowAny
from rest_framework.response import Response
from rest_framework.views import APIView


class SkyObjects(APIView):
    permission_classes = (AllowAny,)

    def get(self, request, f=None, object_type=None):
        print("TYPE=", object_type)
        res = {'list': ['Aldebaran', 'Vega', 'Deneb', 'Altair']}
        return Response(res)
