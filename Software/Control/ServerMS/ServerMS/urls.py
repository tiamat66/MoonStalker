from django.contrib import admin
from django.conf.urls import url
from rest_framework.authtoken import views as auth_views

from sky.rest import SkyObjects

urlpatterns = [
    url(r'^admin/', admin.site.urls),
    url(r'^rest/get-object-list/(?P<object_type>.*?)/?$', SkyObjects.as_view(), name='SkyObjects'),
    url(r'^rest/token/', auth_views.obtain_auth_token),
]
